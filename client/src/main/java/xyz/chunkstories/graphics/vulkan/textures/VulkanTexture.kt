package xyz.chunkstories.graphics.vulkan.textures

import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkImageCreateInfo
import org.lwjgl.vulkan.VkImageMemoryBarrier
import org.lwjgl.vulkan.VkImageViewCreateInfo
import org.lwjgl.vulkan.VkMemoryRequirements
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.graphics.Texture
import xyz.chunkstories.api.graphics.TextureFormat
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.memory.MemoryUsagePattern
import xyz.chunkstories.graphics.vulkan.memory.VulkanMemoryManager
import xyz.chunkstories.graphics.vulkan.resources.Cleanable
import xyz.chunkstories.graphics.vulkan.util.*
import kotlin.concurrent.withLock

open class VulkanTexture(val backend: VulkanGraphicsBackend, final override val format: TextureFormat,
                         private val width: Int, private val height: Int, private val depth: Int, private val layerCount: Int,
                         private val imageType: Int, private val imageViewType: Int, private val usageFlags: Int) : Texture, Cleanable {

    private val vulkanFormat = format.vulkanFormat
    private val allocation: VulkanMemoryManager.Allocation

    val imageHandle: VkImage
    val imageView: VkImageView

    init {
        stackPush()

        val imageCreateInfo = VkImageCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO).apply {
            imageType(imageType)
            extent().width(width)
            extent().height(height)
            extent().depth(depth)
            mipLevels(1)
            arrayLayers(layerCount)

            format(vulkanFormat.ordinal)
            tiling(VK_IMAGE_TILING_OPTIMAL) // TODO are we sure ?

            usage(usageFlags)
            //usage(VK_IMAGE_USAGE_TRANSFER_DST_BIT or VK_IMAGE_USAGE_SAMPLED_BIT)

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

        val usagePattern =
                when {
                    usageFlags and VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT != 0 || usageFlags and VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT != 0 -> MemoryUsagePattern.SEMI_STATIC
                    else -> MemoryUsagePattern.STATIC
                }
        allocation = backend.memoryManager.allocateMemory(memRequirements, usagePattern)

        allocation.lock.withLock {
            vkBindImageMemory(backend.logicalDevice.vkDevice, imageHandle, allocation.deviceMemory, allocation.offset)
        }

        val viewInfo = VkImageViewCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO).apply {
            image(imageHandle)
            viewType(imageViewType)
            format(vulkanFormat.ordinal)
            subresourceRange().apply {
                if (format == TextureFormat.DEPTH_32 || format == TextureFormat.DEPTH_24)
                    aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT)
                else
                    aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                baseMipLevel(0)
                levelCount(1)
                baseArrayLayer(0)
                layerCount(layerCount)
            }
        }

        val pImageView = stackMallocLong(1)
        vkCreateImageView(backend.logicalDevice.vkDevice, viewInfo, null, pImageView)
        imageView = pImageView.get(0)

        stackPop()
    }

    fun transitionLayout(oldLayout: VkImageLayout, newLayout: VkImageLayout) {
        stackPush()
        val operationsPool = backend.logicalDevice.graphicsQueue.threadSafePools.get()
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

            (oldLayout == VK_IMAGE_LAYOUT_UNDEFINED) and (newLayout == VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL) -> {
                // We want to write to the image in the transfer stage
                dstAccessMask = VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT or VK_ACCESS_COLOR_ATTACHMENT_READ_BIT
                dstStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT

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

            else -> logger.error("Unhandled transition : $oldLayout to $newLayout")
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
                layerCount(layerCount)
            }

            srcAccessMask(srcAccessMask)
            dstAccessMask(dstAccessMask)
        }

        vkCmdPipelineBarrier(commandBuffer, srcStage, dstStage, 0, null, null, imageBarrier)

        val fence = backend.createFence(false)
        operationsPool.submitOneTimeCB(commandBuffer, backend.logicalDevice.graphicsQueue, fence)

        backend.waitFence(fence)

        vkDestroyFence(backend.logicalDevice.vkDevice, fence, null)
        vkFreeCommandBuffers(backend.logicalDevice.vkDevice, operationsPool.handle, commandBuffer)

        stackPop()
    }

    override fun cleanup() {
        vkDestroyImageView(backend.logicalDevice.vkDevice, imageView, null)
        vkDestroyImage(backend.logicalDevice.vkDevice, imageHandle, null)

        allocation.cleanup()
    }

    companion object {
        val logger = LoggerFactory.getLogger("client.vulkan")
    }
}