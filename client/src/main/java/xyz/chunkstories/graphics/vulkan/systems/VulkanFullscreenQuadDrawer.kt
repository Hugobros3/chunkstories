package xyz.chunkstories.graphics.vulkan.systems

import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBuffer
import xyz.chunkstories.api.client.IngameClient
import xyz.chunkstories.api.graphics.ImageInput
import xyz.chunkstories.graphics.common.FaceCullingMode
import xyz.chunkstories.graphics.common.Primitive
import xyz.chunkstories.graphics.vulkan.Pipeline
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.buffers.VulkanVertexBuffer
import xyz.chunkstories.graphics.vulkan.graph.VulkanPass
import xyz.chunkstories.graphics.vulkan.resources.DescriptorSetsMegapool
import xyz.chunkstories.graphics.vulkan.swapchain.Frame
import xyz.chunkstories.graphics.vulkan.textures.VulkanSampler
import xyz.chunkstories.graphics.vulkan.vertexInputConfiguration

class VulkanFullscreenQuadDrawer(pass: VulkanPass) : VulkanDrawingSystem(pass) {
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
    val pipeline = Pipeline(backend, pass, vertexInputConfiguration, Primitive.TRIANGLES, FaceCullingMode.CULL_BACK)
    val sampler = VulkanSampler(backend)

    private val vertexBuffer: VulkanVertexBuffer

    init {
        val vertices = floatArrayOf(
                -1.0F, -3.0F,
                3.0F, 1.0F,
                -1.0F, 1.0F
        )

        vertexBuffer = VulkanVertexBuffer(backend, vertices.size * 4L)

        stackPush().use {
            val byteBuffer = stackMalloc(vertices.size * 4)
            vertices.forEach { f -> byteBuffer.putFloat(f) }
            byteBuffer.flip()

            vertexBuffer.upload(byteBuffer)
        }
    }

    var bindings: (VulkanFullscreenQuadDrawer.(DescriptorSetsMegapool.ShaderBindingContext) -> Unit)? = null

    fun shaderBindings(bindings: VulkanFullscreenQuadDrawer.(bindingContext: DescriptorSetsMegapool.ShaderBindingContext) -> Unit) {
        this.bindings = bindings
    }

    override fun registerDrawingCommands(frame: Frame, commandBuffer: VkCommandBuffer) {
        val bindingContext = backend.descriptorMegapool.getBindingContext(pipeline)
        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.handle)
        for (input in pass.imageInputs) {
            val source = input.source
            when (source) {
                is ImageInput.ImageSource.RenderBufferReference -> {
                    bindingContext.bindTextureAndSampler(input.name, pass.graph.buffers[source.renderBufferName]?.texture!!, sampler)
                }
                is ImageInput.ImageSource.AssetReference -> TODO()
                is ImageInput.ImageSource.TextureReference -> TODO()
            }
        }

        //println("pass ${pass.name}  $bindings")
        bindings?.invoke(this, bindingContext)

        vkCmdBindVertexBuffers(commandBuffer, 0, stackLongs(vertexBuffer.handle), stackLongs(0))
        bindingContext.preDraw(commandBuffer)
        vkCmdDraw(commandBuffer, 3 * 1, 1, 0, 0)

        frame.recyclingTasks.add {
            bindingContext.recycle()
        }
    }

    override fun cleanup() {
        sampler.cleanup()

        vertexBuffer.cleanup()
        pipeline.cleanup()
        //descriptorPool.cleanup()
    }
}