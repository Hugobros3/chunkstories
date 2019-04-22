package xyz.chunkstories.graphics.vulkan.systems

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBuffer
import xyz.chunkstories.api.client.IngameClient
import xyz.chunkstories.api.graphics.TextureTilingMode
import xyz.chunkstories.api.graphics.rendergraph.ImageInput
import xyz.chunkstories.api.graphics.rendergraph.ImageSource
import xyz.chunkstories.api.util.kotlin.toVec3d
import xyz.chunkstories.graphics.common.FaceCullingMode
import xyz.chunkstories.graphics.common.Primitive
import xyz.chunkstories.graphics.vulkan.Pipeline
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.buffers.VulkanVertexBuffer
import xyz.chunkstories.graphics.vulkan.graph.VulkanFrameGraph
import xyz.chunkstories.graphics.vulkan.graph.VulkanPass
import xyz.chunkstories.graphics.vulkan.memory.MemoryUsagePattern
import xyz.chunkstories.graphics.vulkan.swapchain.Frame
import xyz.chunkstories.graphics.vulkan.systems.world.ViewportSize
import xyz.chunkstories.graphics.vulkan.systems.world.VulkanWorldVolumetricTexture
import xyz.chunkstories.graphics.vulkan.systems.world.getConditions
import xyz.chunkstories.graphics.vulkan.textures.VulkanSampler
import xyz.chunkstories.graphics.vulkan.vertexInputConfiguration
import xyz.chunkstories.world.WorldClientCommon

class Vulkan3DVoxelRaytracer(pass: VulkanPass) : VulkanDrawingSystem(pass) {
    val backend: VulkanGraphicsBackend
        get() = pass.backend

    val client: IngameClient
        get() = backend.window.client.ingame!!

    val vertexInputConfiguration = vertexInputConfiguration {
        binding {
            binding(0)
            stride(2 * 4)
            inputRate(VK_VERTEX_INPUT_RATE_VERTEX)
        }

        attribute {
            binding(0)
            location(program.vertexInputs.find { it.name == "vertexIn" }?.location!!)
            format(VK_FORMAT_R32G32_SFLOAT)
            offset(0)
        }
    }

    val program = backend.shaderFactory.createProgram("raytraced")
    val pipeline = Pipeline(backend, program, pass, vertexInputConfiguration, Primitive.TRIANGLES, FaceCullingMode.CULL_BACK)
    val sampler = VulkanSampler(backend, tilingMode = TextureTilingMode.REPEAT)

    private val vertexBuffer: VulkanVertexBuffer

    val volumetricTexture = VulkanWorldVolumetricTexture(backend, client.ingame?.world as WorldClientCommon)

    init {
        val vertices = floatArrayOf(
                -1.0F, -3.0F,
                3.0F, 1.0F,
                -1.0F, 1.0F
        )

        vertexBuffer = VulkanVertexBuffer(backend, vertices.size * 4L, MemoryUsagePattern.STATIC)

        MemoryStack.stackPush().use {
            val byteBuffer = MemoryStack.stackMalloc(vertices.size * 4)
            vertices.forEach { f -> byteBuffer.putFloat(f) }
            byteBuffer.flip()

            vertexBuffer.upload(byteBuffer)
        }
    }

    override fun registerDrawingCommands(frame: Frame, commandBuffer: VkCommandBuffer, passContext: VulkanFrameGraph.FrameGraphNode.PassNode) {
        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.handle)
        val bindingContext = backend.descriptorMegapool.getBindingContext(pipeline)

        for (input in pass.declaration.inputs?.imageInputs ?: emptyList<ImageInput>()) {
            val source = input.source
            when (source) {
                is ImageSource.RenderBufferReference -> {
                    bindingContext.bindTextureAndSampler(input.name, pass.renderTask.buffers[source.renderBufferName]?.getAttachementTexture(frame)!!, sampler)
                }
                is ImageSource.AssetReference -> TODO()
                is ImageSource.TextureReference -> TODO()
            }
        }

        volumetricTexture.updateArround(passContext.context.camera.position.toVec3d())

        val viewportSize = ViewportSize()
        viewportSize.size.set(passContext.resolvedOutputs[pass.declaration.outputs.outputs[0]]!!.textureSize)

        bindingContext.bindUBO("viewportSize", viewportSize)
        bindingContext.bindUBO("camera", passContext.context.camera)
        bindingContext.bindUBO("voxelDataInfo", volumetricTexture.info)
        bindingContext.bindUBO("world", volumetricTexture.world.getConditions())
        bindingContext.bindTextureAndSampler("voxelData", volumetricTexture.texture, sampler)

        bindingContext.bindTextureAndSampler("blueNoise", backend.textures.getOrLoadTexture2D("textures/noise/blue1024.png"), sampler)

        bindingContext.preDraw(commandBuffer)

        vkCmdBindVertexBuffers(commandBuffer, 0, MemoryStack.stackLongs(vertexBuffer.handle), MemoryStack.stackLongs(0))
        vkCmdDraw(commandBuffer, 3 * 1, 1, 0, 0)

        frame.recyclingTasks.add {
            bindingContext.recycle()
        }
    }

    override fun cleanup() {
        sampler.cleanup()

        volumetricTexture.cleanup()

        vertexBuffer.cleanup()
        pipeline.cleanup()
        program.cleanup()
    }
}