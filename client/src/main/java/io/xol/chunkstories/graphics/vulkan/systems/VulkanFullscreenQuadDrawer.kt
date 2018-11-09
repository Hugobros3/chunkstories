package io.xol.chunkstories.graphics.vulkan.systems

import io.xol.chunkstories.api.graphics.ImageInput
import io.xol.chunkstories.graphics.vulkan.DescriptorPool
import io.xol.chunkstories.graphics.vulkan.Pipeline
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.buffers.VulkanVertexBuffer
import io.xol.chunkstories.graphics.vulkan.graph.VulkanPass
import io.xol.chunkstories.graphics.vulkan.swapchain.Frame
import io.xol.chunkstories.graphics.vulkan.textures.VulkanSampler
import io.xol.chunkstories.graphics.vulkan.vertexInputConfiguration
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBuffer

class VulkanFullscreenQuadDrawer(pass: VulkanPass) : VulkanDrawingSystem(pass) {
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
    val pipeline = Pipeline(backend, pass, vertexInputConfiguration)
    val descriptorPool = DescriptorPool(backend, pass.program)
    val sampler = VulkanSampler(backend)

    private val vertexBuffer: VulkanVertexBuffer

    init {
        val vertices = floatArrayOf(
                -1.0F, -1.0F,
                -1.0F, 1.0F,
                1.0F, 1.0F,
                -1.0F, -1.0F,
                1.0F, -1.0F,
                1.0F, 1.0F
        )

        vertexBuffer = VulkanVertexBuffer(backend, vertices.size * 4L)

        stackPush().use {
            val byteBuffer = stackMalloc(vertices.size * 4)
            vertices.forEach { f -> byteBuffer.putFloat(f) }
            byteBuffer.flip()

            vertexBuffer.upload(byteBuffer)
        }
    }

    override fun registerDrawingCommands(frame: Frame, commandBuffer: VkCommandBuffer) {
        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.handle)
        for(input in pass.imageInputs) {
            val source = input.source
            when(source) {
                is ImageInput.ImageSource.RenderBufferReference -> {
                    descriptorPool.configureTextureAndSampler(frame, input.name, pass.graph.buffers[source.renderBufferName]?.texture!!, sampler)
                }
            }
        }
        vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.layout, 1, descriptorPool.setsForFrame(frame), null as? IntArray)

        vkCmdBindVertexBuffers(commandBuffer, 0, stackLongs(vertexBuffer.handle), stackLongs(0))
        vkCmdDraw(commandBuffer, 3 * 2, 1, 0, 0)
    }

    override fun cleanup() {
        sampler.cleanup()

        vertexBuffer.cleanup()
        pipeline.cleanup()
        descriptorPool.cleanup()
    }
}