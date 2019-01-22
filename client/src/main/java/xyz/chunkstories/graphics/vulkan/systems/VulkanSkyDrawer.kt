package xyz.chunkstories.graphics.vulkan.systems

import xyz.chunkstories.api.entity.traits.serializable.TraitControllable
import xyz.chunkstories.api.graphics.structs.Camera
import xyz.chunkstories.api.graphics.systems.drawing.DrawingSystem
import xyz.chunkstories.graphics.common.FaceCullingMode
import xyz.chunkstories.graphics.common.Primitive
import xyz.chunkstories.graphics.vulkan.Pipeline
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.buffers.VulkanVertexBuffer
import xyz.chunkstories.graphics.vulkan.graph.VulkanPass
import xyz.chunkstories.graphics.vulkan.swapchain.Frame
import xyz.chunkstories.graphics.vulkan.systems.world.getConditions
import xyz.chunkstories.graphics.vulkan.textures.VulkanSampler
import xyz.chunkstories.graphics.vulkan.vertexInputConfiguration
import xyz.chunkstories.world.WorldClientCommon
import org.lwjgl.system.MemoryStack
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
    val pipeline = Pipeline(backend, pass, pass.program, vertexInputConfiguration, Primitive.TRIANGLES, FaceCullingMode.CULL_BACK)
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
        val bindingContext = backend.descriptorMegapool.getBindingContext(pipeline)

        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.handle)
        vkCmdBindVertexBuffers(commandBuffer, 0, MemoryStack.stackLongs(vertexBuffer.handle), MemoryStack.stackLongs(0))

        val client = backend.window.client.ingame!!

        val entity = client.player.controlledEntity
        val camera = entity?.traits?.get(TraitControllable::class)?.camera ?: Camera()
        val world = client.world as WorldClientCommon

        //descriptorPool.configure(frame, camera)
        bindingContext.bindUBO(camera)
        bindingContext.bindUBO(world.getConditions())

        bindingContext.preDraw(commandBuffer)
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