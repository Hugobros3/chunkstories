package xyz.chunkstories.graphics.vulkan.textures

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkBufferImageCopy
import org.lwjgl.vulkan.VkCommandBuffer
import xyz.chunkstories.api.graphics.TextureFormat
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.buffers.VulkanBuffer
import xyz.chunkstories.graphics.vulkan.util.createFence
import xyz.chunkstories.graphics.vulkan.util.waitFence

class VulkanTexture3D(backend: VulkanGraphicsBackend, format: TextureFormat, val width: Int, val height: Int, val depth: Int, val mipLevelsCount: Int, usageFlags: Int) :
        VulkanTexture(backend, format, width, height, depth, 1, mipLevelsCount, VK_IMAGE_TYPE_3D, VK_IMAGE_VIEW_TYPE_3D, usageFlags) {

    fun copyBufferToImage(buffer: VulkanBuffer) {
        MemoryStack.stackPush()
        val operationsPool = backend.logicalDevice.graphicsQueue.threadSafePools.get()
        val commandBuffer = operationsPool.startPrimaryCommandBuffer()

        copyBufferToImage(commandBuffer, buffer)

        val fence = backend.createFence(false)
        vkEndCommandBuffer(commandBuffer)
        operationsPool.submitAndReturnPrimaryCommandBuffer(commandBuffer, backend.logicalDevice.graphicsQueue, fence)

        backend.waitFence(fence)

        vkDestroyFence(backend.logicalDevice.vkDevice, fence, null)

        operationsPool.returnPrimaryCommandBuffer(commandBuffer)
        //vkFreeCommandBuffers(backend.logicalDevice.vkDevice, operationsPool.handle, commandBuffer)

        MemoryStack.stackPop()
    }

    fun copyBufferToImage(commandBuffer: VkCommandBuffer, buffer: VulkanBuffer) {
        val region = VkBufferImageCopy.callocStack(1).also {
            it.bufferOffset(0)

            // tightly packed
            it.bufferRowLength(0)
            it.bufferImageHeight(0)

            it.imageSubresource().apply {
                aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                mipLevel(0)
                baseArrayLayer(0)
                layerCount(1)
            }

            it.imageOffset().apply {
                x(0)
                y(0)
                z(0)
            }

            it.imageExtent().apply {
                width(width)
                height(height)
                depth(depth)
            }
        }

        vkCmdCopyBufferToImage(commandBuffer, buffer.handle, imageHandle, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region)
    }
}