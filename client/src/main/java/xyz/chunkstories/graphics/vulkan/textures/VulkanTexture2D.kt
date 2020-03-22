package xyz.chunkstories.graphics.vulkan.textures

import org.lwjgl.system.MemoryStack.stackPop
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkBufferImageCopy
import xyz.chunkstories.api.graphics.Texture2D
import xyz.chunkstories.api.graphics.TextureFormat
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.buffers.VulkanBuffer
import xyz.chunkstories.graphics.vulkan.util.createFence
import xyz.chunkstories.graphics.vulkan.util.waitFence

class VulkanTexture2D(backend: VulkanGraphicsBackend, format: TextureFormat, override val width: Int, override val height: Int, usageFlags: Int) :
        VulkanTexture(backend, format,
                width, height, 1, 1, 1,
                VK_IMAGE_TYPE_2D, VK_IMAGE_VIEW_TYPE_2D, usageFlags), Texture2D {

    val mapping: Int by lazy { backend.textures.magicTexturing!!.assignId(this) }

    fun copyBufferToImage(buffer: VulkanBuffer) {
        stackPush()
        val operationsPool = backend.logicalDevice.graphicsQueue.threadSafePools.get()
        val commandBuffer = operationsPool.startPrimaryCommandBuffer()

        val region = VkBufferImageCopy.callocStack(1).apply {
            bufferOffset(0)

            // tightly packed
            bufferRowLength(0)
            bufferImageHeight(0)

            imageSubresource().apply {
                aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                mipLevel(0)
                baseArrayLayer(0)
                layerCount(1)
            }

            imageOffset().apply {
                x(0)
                y(0)
                z(0)
            }

            imageExtent().apply {
                width(width)
                height(height)
                depth(1)
            }
        }

        vkCmdCopyBufferToImage(commandBuffer, buffer.handle, imageHandle, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region)

        val fence = backend.createFence(false)
        vkEndCommandBuffer(commandBuffer)
        operationsPool.submitAndReturnPrimaryCommandBuffer(commandBuffer, backend.logicalDevice.graphicsQueue, fence)

        backend.waitFence(fence)

        vkDestroyFence(backend.logicalDevice.vkDevice, fence, null)

        operationsPool.returnPrimaryCommandBuffer(commandBuffer)
        //vkFreeCommandBuffers(backend.logicalDevice.vkDevice, operationsPool.handle, commandBuffer)

        stackPop()
    }


}
