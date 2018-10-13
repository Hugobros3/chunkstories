package io.xol.chunkstories.graphics.vulkan

import io.xol.chunkstories.graphics.vulkan.swapchain.PerFrameResource
import io.xol.chunkstories.graphics.vulkan.swapchain.SwapChain
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import org.slf4j.LoggerFactory

class TriangleDrawer(val backend: VulkanGraphicsBackend) {

    val baseProgram = backend.shaderFactory.createProgram(backend, "/shaders/blit")

    val pipeline = Pipeline(backend, backend.renderToBackbuffer, baseProgram)
    val cmdPool = CommandPool(backend, backend.logicalDevice.graphicsQueue.family, VK_COMMAND_POOL_CREATE_TRANSIENT_BIT  or VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)

    val vertexBuffers: PerFrameResource<VulkanVertexBuffer>
    val commandBuffers: PerFrameResource<VkCommandBuffer>

    init {
        stackPush()

        vertexBuffers = PerFrameResource(backend) {
            VulkanVertexBuffer(backend, 1024)
        }

        commandBuffers = PerFrameResource(backend) {

            val commandBufferAllocateInfo = VkCommandBufferAllocateInfo.callocStack().sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO).apply {
                commandPool(cmdPool.handle)
                level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                commandBufferCount(1)
            }

            val pCmdBuffers = stackMallocPointer(1)
            vkAllocateCommandBuffers(backend.logicalDevice.vkDevice, commandBufferAllocateInfo, pCmdBuffers)

            val commandBuffer = VkCommandBuffer(pCmdBuffers.get(0), backend.logicalDevice.vkDevice)

            commandBuffer
        }

        stackPop()
    }

    fun drawTriangle(frame : SwapChain.Frame) {
        stackPush().use {

            // Rewrite the vertex buffer
            vertexBuffers[frame].apply {
                val data = floatArrayOf(0.0F + Math.sin(frame.frameNumber * 0.01).toFloat(), -0.5F, 0.5F, 0.5F, -0.5F, 0.5F)
                val byteBuffer = stackMalloc(data.size * 4)

                val fb = byteBuffer.asFloatBuffer()

                fb.put(data)
                this.upload(byteBuffer)
            }

            // Rewrite the command buffer
            commandBuffers[frame].apply {
                val beginInfo = VkCommandBufferBeginInfo.callocStack().sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO).apply {
                    flags(VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT)
                    pInheritanceInfo(null)
                }

                vkBeginCommandBuffer(this, beginInfo)

                val renderPassBeginInfo = VkRenderPassBeginInfo.callocStack().sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO).apply {
                    renderPass(backend.renderToBackbuffer.handle)

                    //TODO frame.swapchainImage
                    framebuffer(backend.swapchain.swapChainFramebuffers[frame.swapchainImageIndex])
                    renderArea().offset().x(0)
                    renderArea().offset().y(0)
                    renderArea().extent().width(backend.window.width)
                    renderArea().extent().height(backend.window.height)
                    //renderArea().extent(backend.physicalDevice.swapchainDetails.swapExtentToUse)

                    val clearColor = VkClearValue.callocStack(1).apply {
                        color().float32().apply {
                            this.put(0, 0.0F)
                            this.put(1, 0.5F)
                            this.put(2, 0.0F)
                            this.put(3, 1.0F)
                        }
                    }
                    pClearValues(clearColor)
                }

                vkCmdBeginRenderPass(this, renderPassBeginInfo, VK_SUBPASS_CONTENTS_INLINE)
                vkCmdBindPipeline(this, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.handle)

                vkCmdBindVertexBuffers(this, 0, stackLongs(vertexBuffers[frame].handle), stackLongs(0))

                vkCmdDraw(this, 3, 1, 0, 0) // that's rather anticlimactic
                vkCmdEndRenderPass(this)

                vkEndCommandBuffer(this)
            }

            val submitInfo = VkSubmitInfo.callocStack().sType(VK_STRUCTURE_TYPE_SUBMIT_INFO).apply {
                val waitOnSemaphores = stackMallocLong(1)
                waitOnSemaphores.put(0, frame.renderCanBeginSemaphore)
                pWaitSemaphores(waitOnSemaphores)
                waitSemaphoreCount(1)

                val waitStages = stackMallocInt(1)
                waitStages.put(0, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                pWaitDstStageMask(waitStages)

                val commandBuffers = stackMallocPointer(1)
                commandBuffers.put(0, this@TriangleDrawer.commandBuffers[frame])
                pCommandBuffers(commandBuffers)

                val semaphoresToSignal = stackLongs(frame.renderFinishedSemaphore)
                pSignalSemaphores(semaphoresToSignal)
            }

            vkQueueSubmit(backend.logicalDevice.graphicsQueue.handle, submitInfo, frame.renderFinishedFence).ensureIs("Failed to submit command buffer", VK_SUCCESS)
        }
    }

    fun cleanup() {
        vertexBuffers.cleanup()

        cmdPool.cleanup()
        pipeline.cleanup()

        baseProgram.cleanup()
    }

    companion object {
        val logger = LoggerFactory.getLogger("client.vulkan.triangleTest")
    }

}