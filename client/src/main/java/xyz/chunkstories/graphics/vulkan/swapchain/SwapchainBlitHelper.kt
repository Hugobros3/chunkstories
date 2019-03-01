package xyz.chunkstories.graphics.vulkan.swapchain

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR
import org.lwjgl.vulkan.VK10.*
import xyz.chunkstories.graphics.vulkan.CommandPool
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.graph.VulkanRenderBuffer
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.vulkan.resources.InflightFrameResource
import xyz.chunkstories.graphics.vulkan.util.ensureIs

class SwapchainBlitHelper(val backend: VulkanGraphicsBackend) : Cleanable {

    val commandPool: CommandPool
    val commandBuffers: InflightFrameResource<VkCommandBuffer>

    init {
        commandPool = CommandPool(backend, backend.logicalDevice.graphicsQueue.family, VK_COMMAND_POOL_CREATE_TRANSIENT_BIT or VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
        commandBuffers = InflightFrameResource(backend) {
            val commandBufferAllocateInfo = VkCommandBufferAllocateInfo.callocStack().sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO).apply {
                commandPool(commandPool.handle)
                level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                commandBufferCount(1)
            }

            val pCmdBuffers = MemoryStack.stackMallocPointer(1)
            vkAllocateCommandBuffers(backend.logicalDevice.vkDevice, commandBufferAllocateInfo, pCmdBuffers)

            val commandBuffer = VkCommandBuffer(pCmdBuffers.get(0), backend.logicalDevice.vkDevice)

            commandBuffer
        }
    }

    override fun cleanup() {
        commandPool.cleanup()
    }

    fun copyFinalRenderbuffer(frame: Frame, finalRenderBuffer: VulkanRenderBuffer)  {
        stackPush().use {
            commandBuffers[frame].apply {
                val beginInfo = VkCommandBufferBeginInfo.callocStack().sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO).apply {
                    flags(VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT)
                    pInheritanceInfo(null)
                }

                vkBeginCommandBuffer(this, beginInfo)

                val barriers = VkImageMemoryBarrier.callocStack(2)

                val finalRenderBufferReadyCopyBarrier = barriers[1].sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER).apply {
                    //TODO check last use
                    oldLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                    newLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL)
                    srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)

                    image(finalRenderBuffer.texture.imageHandle)

                    subresourceRange().apply {
                        aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        baseMipLevel(0)
                        levelCount(1)
                        baseArrayLayer(0)
                        layerCount(1)
                    }

                    srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT or VK_ACCESS_COLOR_ATTACHMENT_READ_BIT)
                    dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT)
                }
                val swapchainImageReadyCopyBarrier = barriers[0].sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER).apply {
                    oldLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    newLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                    srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)

                    image(frame.swapchainImage)

                    subresourceRange().apply {
                        aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        baseMipLevel(0)
                        levelCount(1)
                        baseArrayLayer(0)
                        layerCount(1)
                    }

                    srcAccessMask(0)
                    dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                }
                vkCmdPipelineBarrier(this, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT, 0, null, null, barriers)

                val region = VkImageCopy.callocStack(1).apply {
                    this.extent().width(finalRenderBuffer.size.x)
                    this.extent().height(finalRenderBuffer.size.y)
                    this.extent().depth(1)

                    srcSubresource().apply {
                        aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        mipLevel(0)
                        baseArrayLayer(0)
                        layerCount(1)
                    }

                    dstSubresource().set(srcSubresource())
                }

                vkCmdCopyImage(this, finalRenderBuffer.texture.imageHandle, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                        frame.swapchainImage, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region)

                val swapchainImageReadyPresentBarrier = VkImageMemoryBarrier.callocStack(1).sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER).apply {
                    oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                    newLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
                    srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    image(frame.swapchainImage)

                    subresourceRange().apply {
                        aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        baseMipLevel(0)
                        levelCount(1)
                        baseArrayLayer(0)
                        layerCount(1)
                    }

                    srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                    dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT)
                }

                vkCmdPipelineBarrier(this, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, 0, null, null, swapchainImageReadyPresentBarrier)

                //vkCmdEndRenderPass(this)
                vkEndCommandBuffer(this)
            }

            val submitInfo = VkSubmitInfo.callocStack().sType(VK_STRUCTURE_TYPE_SUBMIT_INFO).apply {
                val commandBuffers = MemoryStack.stackMallocPointer(1)
                commandBuffers.put(0, this@SwapchainBlitHelper.commandBuffers[frame])
                pCommandBuffers(commandBuffers)

                /*val waitOnSemaphores = MemoryStack.stackMallocLong(1)
                waitOnSemaphores.put(0, frame.renderCanBeginSemaphore)
                pWaitSemaphores(waitOnSemaphores)
                waitSemaphoreCount(1)

                val waitStages = MemoryStack.stackMallocInt(1)
                waitStages.put(0, VK_PIPELINE_STAGE_TRANSFER_BIT)
                pWaitDstStageMask(waitStages)*/

                val semaphoresToSignal = MemoryStack.stackLongs(frame.renderFinishedSemaphore)
                pSignalSemaphores(semaphoresToSignal)
            }

            backend.logicalDevice.graphicsQueue.mutex.acquireUninterruptibly()
            vkQueueSubmit(backend.logicalDevice.graphicsQueue.handle, submitInfo, frame.renderFinishedFence).ensureIs("Failed to submit command buffer", VK_SUCCESS)
            backend.logicalDevice.graphicsQueue.mutex.release()
        }
    }
}