package io.xol.chunkstories.graphics.vulkan

import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import org.slf4j.LoggerFactory

class TriangleDrawer(val backend: VulkanGraphicsBackend) {

    val vertexShaderModule = ShaderModule(backend, javaClass.classLoader.getResourceAsStream("./shaders/base.vert.spv"))
    val fragmentShaderModule = ShaderModule(backend, javaClass.classLoader.getResourceAsStream("./shaders/base.frag.spv"))

    val pipeline = Pipeline(backend, backend.renderToBackbuffer, vertexShaderModule, fragmentShaderModule)
    val cmdPool = CommandPool(backend, backend.logicalDevice.graphicsQueue.family)

    val commandBuffers : List<VkCommandBuffer>

    private val imagesCount: Int

    init {
        stackPush()

        imagesCount = backend.swapchain.imagesCount

        // Allocate as many command buffers as there are images in the swapchain (the commands differ as you render to another target)
        val commandBufferAllocateInfo = VkCommandBufferAllocateInfo.callocStack().sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO).apply {
            commandPool(cmdPool.handle)
            level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
            commandBufferCount(imagesCount)
        }

        logger.info("Allocating command buffers")
        val pCmdBuffers = stackMallocPointer(imagesCount)
        vkAllocateCommandBuffers(backend.logicalDevice.vkDevice, commandBufferAllocateInfo, pCmdBuffers)
        commandBuffers = mutableListOf()
        for(pCmdBuffer in pCmdBuffers) {
            commandBuffers += VkCommandBuffer(pCmdBuffer, backend.logicalDevice.vkDevice)
        }

        // Record the necessary commands in those command buffers
        for((i, commandBuffer) in commandBuffers.withIndex()) {
            logger.debug("Filling command buffer $i $commandBuffer")
            val beginInfo = VkCommandBufferBeginInfo.callocStack().sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO).apply {
                flags(VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT)
                pInheritanceInfo(null)
            }

            vkBeginCommandBuffer(commandBuffer, beginInfo)

            val renderPassBeginInfo = VkRenderPassBeginInfo.callocStack().sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO).apply {
                renderPass(backend.renderToBackbuffer.handle)
                framebuffer(backend.swapchain.swapChainFramebuffers[i])
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

            vkCmdBeginRenderPass(commandBuffer, renderPassBeginInfo, VK_SUBPASS_CONTENTS_INLINE)
            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.handle)
            vkCmdDraw(commandBuffer, 3, 1, 0, 0) // that's rather anticlimactic
            vkCmdEndRenderPass(commandBuffer)

            vkEndCommandBuffer(commandBuffer)
        }

        stackPop()
    }

    fun drawTriangle(frame : SwapChain.Frame) {
        stackPush().use {

            val submitInfo = VkSubmitInfo.callocStack().sType(VK_STRUCTURE_TYPE_SUBMIT_INFO).apply {
                val waitOnSemaphores = stackMallocLong(1)
                waitOnSemaphores.put(0, frame.renderCanBeginSemaphore)
                pWaitSemaphores(waitOnSemaphores)
                waitSemaphoreCount(1)

                val waitStages = stackMallocInt(1)
                waitStages.put(0, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                pWaitDstStageMask(waitStages)

                val commandBuffers = stackMallocPointer(1)
                commandBuffers.put(0, this@TriangleDrawer.commandBuffers[frame.swapchainImageIndex])
                pCommandBuffers(commandBuffers)

                val semaphoresToSignal = stackLongs(frame.renderFinishedSemaphore)
                pSignalSemaphores(semaphoresToSignal)
            }

            vkQueueSubmit(backend.logicalDevice.graphicsQueue.handle, submitInfo, frame.renderFinishedFence).ensureIs("Failed to submit command buffer", VK_SUCCESS)
        }
    }

    fun cleanup() {
        cmdPool.cleanup()
        pipeline.cleanup()

        fragmentShaderModule.cleanup()
        vertexShaderModule.cleanup()
    }

    companion object {
        val logger = LoggerFactory.getLogger("client.vulkan.triangleTest")
    }

}