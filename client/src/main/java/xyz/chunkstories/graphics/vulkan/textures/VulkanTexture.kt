package xyz.chunkstories.graphics.vulkan.textures

import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRDedicatedAllocation.*
import org.lwjgl.vulkan.KHRGetMemoryRequirements2.*
import org.lwjgl.vulkan.VK10.*
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.graphics.Texture
import xyz.chunkstories.api.graphics.TextureFormat
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.graph.VulkanPass
import xyz.chunkstories.graphics.vulkan.memory.MemoryUsagePattern
import xyz.chunkstories.graphics.vulkan.memory.VulkanMemoryManager
import xyz.chunkstories.graphics.vulkan.util.*
import kotlin.concurrent.withLock

open class VulkanTexture(val backend: VulkanGraphicsBackend, final override val format: TextureFormat,
                         private val width: Int, private val height: Int, private val depth: Int, private val layerCount: Int, private val mipLevelsCount: Int,
                         private val imageType: Int, private val imageViewType: Int, private val usageFlags: Int) : Texture, Cleanable {

    private val vulkanFormat = format.vulkanFormat

    private var dedicatedMemory: VkDeviceMemory = -1
    private val sharedAllocation: VulkanMemoryManager.Allocation?

    val imageHandle: VkImage
    val imageView: VkImageView

    init {
        stackPush()

        val imageCreateInfo = VkImageCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO).apply {
            imageType(imageType)
            extent().width(width)
            extent().height(height)
            extent().depth(depth)
            mipLevels(mipLevelsCount)
            arrayLayers(layerCount)

            format(vulkanFormat.ordinal)
            tiling(VK_IMAGE_TILING_OPTIMAL) // TODO are we sure ?

            usage(usageFlags)
            //usage(VK_IMAGE_USAGE_TRANSFER_DST_BIT or VK_IMAGE_USAGE_SAMPLED_BIT)

            initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
            sharingMode(VK_SHARING_MODE_EXCLUSIVE)
            samples(VK_SAMPLE_COUNT_1_BIT)

            var flags = 0

            if(imageViewType == VK_IMAGE_VIEW_TYPE_CUBE || imageViewType == VK_IMAGE_VIEW_TYPE_CUBE_ARRAY)
                flags = flags or VK_IMAGE_CREATE_CUBE_COMPATIBLE_BIT

            flags(flags)
        }

        val pImageHandle = stackMallocLong(1)
        vkCreateImage(backend.logicalDevice.vkDevice, imageCreateInfo, null, pImageHandle).ensureIs("Failed to create image $this", VK_SUCCESS)

        imageHandle = pImageHandle.get(0)

        val usagePattern =
                when {
                    usageFlags and VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT != 0 || usageFlags and VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT != 0 -> MemoryUsagePattern.SEMI_STATIC
                    else -> MemoryUsagePattern.SEMI_STATIC
                    //else -> MemoryUsagePattern.STATIC
                }

        val eligibleForDedicatedAllocation = ((usageFlags and VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT) != 0) || ((usageFlags and VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT) != 0)

        val useDedicatedAllocation: Boolean
        if (eligibleForDedicatedAllocation) {
            //println("texture $this is eligible for dedicated allocation")
            val memReqInfo2 = VkImageMemoryRequirementsInfo2.callocStack().apply {
                sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_REQUIREMENTS_INFO_2_KHR)
                this.image(imageHandle)
            }

            val memReq2 = VkMemoryRequirements2.callocStack().apply {
                sType(VK_STRUCTURE_TYPE_MEMORY_REQUIREMENTS_2_KHR)
            }

            val memDedicatedReq = VkMemoryDedicatedRequirements.callocStack().apply {
                sType(VK_STRUCTURE_TYPE_MEMORY_DEDICATED_REQUIREMENTS_KHR)
            }

            memReq2.pNext(memDedicatedReq.address())

            vkGetImageMemoryRequirements2KHR(backend.logicalDevice.vkDevice, memReqInfo2, memReq2)
            useDedicatedAllocation = memDedicatedReq.prefersDedicatedAllocation() || memDedicatedReq.requiresDedicatedAllocation() || true
            //println("result is $useDedicatedAllocation")

            if (useDedicatedAllocation) {
                val (memoryTypeIndex, memoryType) = backend.memoryManager.findMemoryTypeToUse(memReq2.memoryRequirements().memoryTypeBits(), VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT)

                val allocateInfo = VkMemoryAllocateInfo.callocStack().apply {
                    sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    allocationSize(memReq2.memoryRequirements().size())
                    memoryTypeIndex(memoryTypeIndex)
                }

                val dedicatedAllocateInfo = VkMemoryDedicatedAllocateInfo.callocStack().apply {
                    sType(VK_STRUCTURE_TYPE_MEMORY_DEDICATED_ALLOCATE_INFO_KHR)
                    image(imageHandle)
                }

                allocateInfo.pNext(dedicatedAllocateInfo.address())

                val pDedicatedMemory = stackLongs(0)
                vkAllocateMemory(backend.logicalDevice.vkDevice, allocateInfo, null, pDedicatedMemory)
                dedicatedMemory = pDedicatedMemory[0]
                vkBindImageMemory(backend.logicalDevice.vkDevice, imageHandle, dedicatedMemory, 0)
                logger.info("Successfully allocated ${memReq2.memoryRequirements().size()} dedicated bytes of DEVICE_LOCAL memory for texture $this")
            }
        } else useDedicatedAllocation = false

        if (useDedicatedAllocation) {
            sharedAllocation = null
        } else {
            val memRequirements = VkMemoryRequirements.callocStack()
            vkGetImageMemoryRequirements(backend.logicalDevice.vkDevice, imageHandle, memRequirements)

            if(usagePattern == MemoryUsagePattern.SEMI_STATIC) {
                //println("${memRequirements.size()} alignement: ${memRequirements.alignment()}")
                //Thread.dumpStack()
            }

            sharedAllocation = backend.memoryManager.allocateMemory(memRequirements, usagePattern)
            sharedAllocation.lock.withLock {
                vkBindImageMemory(backend.logicalDevice.vkDevice, imageHandle, sharedAllocation.deviceMemory, sharedAllocation.offset)
            }
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
                levelCount(mipLevelsCount)
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
        val commandBuffer = operationsPool.startPrimaryCommandBuffer()

        transitionLayout(commandBuffer, oldLayout, newLayout)

        val fence = backend.createFence(false)
        vkEndCommandBuffer(commandBuffer)
        operationsPool.submitAndReturnPrimaryCommandBuffer(commandBuffer, backend.logicalDevice.graphicsQueue, fence)

        backend.waitFence(fence)

        vkDestroyFence(backend.logicalDevice.vkDevice, fence, null)

        operationsPool.returnPrimaryCommandBuffer(commandBuffer)
        //vkFreeCommandBuffers(backend.logicalDevice.vkDevice, operationsPool.handle, commandBuffer)

        stackPop()
    }

    private fun transitionLayout(commandBuffer: VkCommandBuffer, oldLayout: VkImageLayout, newLayout: VkImageLayout) {
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

            (oldLayout == VK_IMAGE_LAYOUT_UNDEFINED) and (newLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) -> {
                // We want to write to the image in the transfer stage
                dstAccessMask = VK_ACCESS_SHADER_READ_BIT
                dstStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT

                // There is nothing writing to this image before us
                srcAccessMask = 0
                srcStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT
            }

            else -> logger.error("Unhandled transition : $oldLayout to $newLayout")
        }

        val imageBarrier = VkImageMemoryBarrier.callocStack(1).sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER).also {
            it.oldLayout(oldLayout)
            it.newLayout(newLayout)
            it.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            it.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            it.image(imageHandle)

            it.subresourceRange().apply {
                aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                baseMipLevel(0)
                levelCount(mipLevelsCount)
                baseArrayLayer(0)
                layerCount(layerCount)
            }

            it.srcAccessMask(srcAccessMask)
            it.dstAccessMask(dstAccessMask)
        }

        vkCmdPipelineBarrier(commandBuffer, srcStage, dstStage, 0, null, null, imageBarrier)
    }

    override fun cleanup() {
        vkDestroyImageView(backend.logicalDevice.vkDevice, imageView, null)
        vkDestroyImage(backend.logicalDevice.vkDevice, imageHandle, null)

        sharedAllocation?.cleanup()
        if (dedicatedMemory != -1L) {
            VK10.vkFreeMemory(backend.logicalDevice.vkDevice, dedicatedMemory, null)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VulkanTexture

        if (imageHandle != other.imageHandle) return false
        if (imageView != other.imageView) return false

        return true
    }

    override fun hashCode(): Int {
        var result = imageHandle.hashCode()
        result = 31 * result + imageView.hashCode()
        return result
    }

    companion object {
        val logger = LoggerFactory.getLogger("client.vulkan")
    }
}
