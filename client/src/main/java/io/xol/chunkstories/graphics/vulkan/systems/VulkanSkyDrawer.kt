package io.xol.chunkstories.graphics.vulkan.systems

import io.xol.chunkstories.api.entity.traits.serializable.TraitControllable
import io.xol.chunkstories.api.graphics.ImageInput
import io.xol.chunkstories.api.graphics.structs.Camera
import io.xol.chunkstories.api.graphics.systems.drawing.DrawingSystem
import io.xol.chunkstories.graphics.common.FaceCullingMode
import io.xol.chunkstories.graphics.common.Primitive
import io.xol.chunkstories.graphics.vulkan.DescriptorPool
import io.xol.chunkstories.graphics.vulkan.Pipeline
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.buffers.VulkanVertexBuffer
import io.xol.chunkstories.graphics.vulkan.graph.VulkanPass
import io.xol.chunkstories.graphics.vulkan.swapchain.Frame
import io.xol.chunkstories.graphics.vulkan.systems.world.getConditions
import io.xol.chunkstories.graphics.vulkan.textures.VulkanSampler
import io.xol.chunkstories.graphics.vulkan.vertexInputConfiguration
import io.xol.chunkstories.world.WorldClientCommon
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBuffer

interface SkyDrawer : DrawingSystem

class VulkanSkyDrawer(pass: VulkanPass) : VulkanDrawingSystem(pass), SkyDrawer {
    val backend: VulkanGraphicsBackend
        get() = pass.backend

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
                -1.0F, -1.0F,
                1.0F, 1.0F,
                -1.0F, 1.0F,
                -1.0F, -1.0F,
                1.0F, -1.0F,
                1.0F, 1.0F
        )

        vertexBuffer = VulkanVertexBuffer(backend, vertices.size * 4L)

        MemoryStack.stackPush().use {
            val byteBuffer = MemoryStack.stackMalloc(vertices.size * 4)
            vertices.forEach { f -> byteBuffer.putFloat(f) }
            byteBuffer.flip()

            vertexBuffer.upload(byteBuffer)
        }
    }

    override fun registerDrawingCommands(frame: Frame, commandBuffer: VkCommandBuffer) {
        val bindingContext = backend.descriptorMegapool.getBindingContext(frame, pipeline, commandBuffer)

        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.handle)
        vkCmdBindVertexBuffers(commandBuffer, 0, MemoryStack.stackLongs(vertexBuffer.handle), MemoryStack.stackLongs(0))

        val client = backend.window.client.ingame!!

        val entity = client.player.controlledEntity
        val camera = entity?.traits?.get(TraitControllable::class)?.camera ?: Camera()
        val world = client.world as WorldClientCommon

        //descriptorPool.configure(frame, camera)
        bindingContext.bindUBO(camera)
        bindingContext.bindUBO(world.getConditions())

        bindingContext.preDraw()
        vkCmdDraw(commandBuffer, 3 * 2, 1, 0, 0)

        frame.recyclingTasks.add() {
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