package io.xol.chunkstories.graphics.vulkan.textures

import io.xol.chunkstories.api.graphics.Texture2D
import io.xol.chunkstories.api.graphics.TextureFormat
import io.xol.chunkstories.graphics.vulkan.CommandPool
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.buffers.VulkanBuffer
import io.xol.chunkstories.graphics.vulkan.resources.Cleanable
import io.xol.chunkstories.graphics.vulkan.util.*
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

class VulkanTexture2D(val backend: VulkanGraphicsBackend, val operationsPool: CommandPool, override val format: TextureFormat, override val height: Int, override val width: Int,
                      val usageFlags: Int) : Texture2D, Cleanable {

    val vulkanFormat = format.vulkanFormat

    val imageHandle: VkImage
    val imageMemory: VkDeviceMemory
    val imageView: VkImageView

    init {
        stackPush()

        val imageCreateInfo = VkImageCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO).apply {
            imageType(VK_IMAGE_TYPE_2D)
            extent().width(width)
            extent().height(height)
            extent().depth(1)
            mipLevels(1)
            arrayLayers(1)

            format(vulkanFormat.ordinal)
            tiling(VK_IMAGE_TILING_OPTIMAL) // TODO are we sure ?

            usage(VK_IMAGE_USAGE_TRANSFER_DST_BIT or VK_IMAGE_USAGE_SAMPLED_BIT)

            initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
            sharingMode(VK_SHARING_MODE_EXCLUSIVE)
            samples(VK_SAMPLE_COUNT_1_BIT)
            flags(0)
        }

        val pImageHandle = stackMallocLong(1)
        vkCreateImage(backend.logicalDevice.vkDevice, imageCreateInfo, null, pImageHandle).ensureIs("Failed to create image $this", VK_SUCCESS)

        imageHandle = pImageHandle.get(0)

        val memRequirements = VkMemoryRequirements.callocStack()
        vkGetImageMemoryRequirements(backend.logicalDevice.vkDevice, imageHandle, memRequirements)

        imageMemory = backend.memoryManager.allocateMemoryGivenRequirements(memRequirements,  VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT)

        vkBindImageMemory(backend.logicalDevice.vkDevice, imageHandle, imageMemory, 0)

        val viewInfo = VkImageViewCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO).apply {
            image(imageHandle)
            viewType(VK_IMAGE_VIEW_TYPE_2D)
            format(vulkanFormat.ordinal)
            subresourceRange().apply {
                aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                baseMipLevel(0)
                levelCount(1)
                baseArrayLayer(0)
                layerCount(1)
            }
        }

        val pImageView= stackMallocLong(1)
        vkCreateImageView(backend.logicalDevice.vkDevice, viewInfo, null, pImageView)
        imageView = pImageView.get(0)

        stackPop()
    }

    fun transitionLayout(oldLayout: VkImageLayout, newLayout: VkImageLayout) {
        stackPush()
        val commandBuffer = operationsPool.createOneUseCB()

        // What we want to make sure isn't interfered with
        var dstAccessMask = 0
        var dstStage = 0

        // What is hazardous and we need to wait to finish
        var srcAccessMask = 0
        var srcStage = 0

        when {
            (oldLayout == VK_IMAGE_LAYOUT_UNDEFINED) and (newLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) -> {
                // We want to write to the image in the transfer stage
                dstAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT
                dstStage = VK_PIPELINE_STAGE_TRANSFER_BIT

                // There is nothing writing to this image before us
                srcAccessMask = 0
                srcStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT
            }

            (oldLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) and (newLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) -> {
                // We want the shader reads in the fragment shader to be left alone !
                dstAccessMask = VK_ACCESS_SHADER_READ_BIT
                dstStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT

                // So finish the transfer first.
                srcAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT
                srcStage = VK_PIPELINE_STAGE_TRANSFER_BIT
            }
        }

        val imageBarrier = VkImageMemoryBarrier.callocStack(1).sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER).apply {
            oldLayout(oldLayout)
            newLayout(newLayout)
            srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            image(imageHandle)

            subresourceRange().apply {
                aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                baseMipLevel(0)
                levelCount(1)
                baseArrayLayer(0)
                layerCount(1)
            }

            srcAccessMask(srcAccessMask)
            dstAccessMask(dstAccessMask)
        }

        vkCmdPipelineBarrier(commandBuffer, srcStage, dstStage, 0, null, null, imageBarrier)

        operationsPool.submitOneTimeCB(commandBuffer, backend.logicalDevice.graphicsQueue)

        stackPop()
    }

    fun copyBufferToImage(buffer: VulkanBuffer) {
        stackPush()
        val commandBuffer = operationsPool.createOneUseCB()

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

        operationsPool.submitOneTimeCB(commandBuffer, backend.logicalDevice.graphicsQueue)

        stackPop()
    }

    override fun cleanup() {
        vkDestroyImageView(backend.logicalDevice.vkDevice, imageView, null)

        vkDestroyImage(backend.logicalDevice.vkDevice, imageHandle, null)
        vkFreeMemory(backend.logicalDevice.vkDevice, imageMemory, null)
    }
}
