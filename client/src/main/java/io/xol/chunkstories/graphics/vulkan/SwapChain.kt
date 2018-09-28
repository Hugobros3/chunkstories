package io.xol.chunkstories.graphics.vulkan

import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK10.*
import org.slf4j.LoggerFactory

class SwapChain(val backend: VulkanGraphicsBackend, displayRenderPass: VulkanRenderPass) {

    val handle : Long
    val swapChainImages : List<VkImage>
    val swapChainImageViews : List<VkImageView>
    lateinit var swapChainFramebuffers : List<VkFramebuffer>
        private set

    internal var imagesCount: Int
        private set

    init {
        logger.debug("Creating swapchain...")
        stackPush()

        imagesCount = backend.physicalDevice.swapchainDetails.imageCount.first + 1
        if(imagesCount > backend.physicalDevice.swapchainDetails.imageCount.last)
            imagesCount = backend.physicalDevice.swapchainDetails.imageCount.last
        logger.debug("Asking for $imagesCount in the swapchain")

        val vkSwapchainCreateInfoKHR = VkSwapchainCreateInfoKHR.callocStack().sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR).apply {
            surface(backend.surface.handle)
            minImageCount(imagesCount)
            imageFormat(backend.physicalDevice.swapchainDetails.formatToUse.ordinal)
            imageColorSpace(backend.physicalDevice.swapchainDetails.colorSpaceToUse)
            imageExtent(backend.physicalDevice.swapchainDetails.swapExtentToUse)
            imageArrayLayers(1)
            //TODO maybe not needed once we do everything in offscreen buffers
            imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)

            preTransform(backend.physicalDevice.swapchainDetails.transformToUse)
            compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
            clipped(true)
            oldSwapchain( VK_NULL_HANDLE )
        }

        if (backend.logicalDevice.graphicsQueue == backend.logicalDevice.presentationQueue) {
            vkSwapchainCreateInfoKHR.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE)
        } else {
            vkSwapchainCreateInfoKHR.imageSharingMode(VK_SHARING_MODE_CONCURRENT)
            val pIndices = stackMallocInt(2)
            pIndices.put(intArrayOf(backend.logicalDevice.graphicsQueue.family.index, backend.logicalDevice.presentationQueue.family.index), 0, 2)
            pIndices.flip()
            vkSwapchainCreateInfoKHR.pQueueFamilyIndices(pIndices)
        }

        val pSwapchain = stackMallocLong(1)
        vkCreateSwapchainKHR(backend.logicalDevice.vkDevice, vkSwapchainCreateInfoKHR, null, pSwapchain).ensureIs("Failed to create swapchain", VK_SUCCESS)
        handle = pSwapchain.get(0)
        logger.debug("swapchain successfully created, handle=$handle")

        val pImageCount = stackCallocInt(1)
        vkGetSwapchainImagesKHR(backend.logicalDevice.vkDevice, handle, pImageCount, null)
        val pImages = stackCallocLong(pImageCount.get(0))
        vkGetSwapchainImagesKHR(backend.logicalDevice.vkDevice, handle, pImageCount, pImages)

        val imagesArray = LongArray(pImages.capacity())
        pImages.get(imagesArray)

        swapChainImages = imagesArray.toList()
        logger.debug("grabbed ${swapChainImages.size} swap chain images : $swapChainImages")

        // Create the views & framebuffers
        swapChainImageViews = mutableListOf()
        for(image in swapChainImages) {
            val vkImageViewCreateInfo = VkImageViewCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO).apply {
                image(image)
                viewType(VK_IMAGE_VIEW_TYPE_2D)
                format(backend.physicalDevice.swapchainDetails.formatToUse.ordinal)
                components().r(VK_COMPONENT_SWIZZLE_IDENTITY)
                components().g(VK_COMPONENT_SWIZZLE_IDENTITY)
                components().b(VK_COMPONENT_SWIZZLE_IDENTITY)
                components().a(VK_COMPONENT_SWIZZLE_IDENTITY)
                subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                subresourceRange().baseMipLevel(0)
                subresourceRange().levelCount(1)
                subresourceRange().baseArrayLayer(0)
                subresourceRange().layerCount(1)
            }

            val pImageView = stackMallocLong(1)
            vkCreateImageView(backend.logicalDevice.vkDevice, vkImageViewCreateInfo, null, pImageView)
            swapChainImageViews += pImageView.get(0)

        }

        logger.debug("Created views for those")

        createFramebuffers(displayRenderPass)

        stackPop()
    }

    private fun createFramebuffers(displayRenderPass: VulkanRenderPass) {
        stackPush()
        swapChainFramebuffers = mutableListOf()

        for(imageView in swapChainImageViews) {
            val pAttachement = stackMallocLong(1)
            pAttachement.put(0, imageView)

            val framebufferCreateInfo = VkFramebufferCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO).apply {
                renderPass(displayRenderPass.handle)
                pAttachments(pAttachement)
                width(backend.window.width)
                height(backend.window.height)
                layers(1)
            }

            val pFramebuffer = stackMallocLong(1)
            vkCreateFramebuffer(backend.logicalDevice.vkDevice, framebufferCreateInfo, null, pFramebuffer).ensureIs("Failed to create framebuffer", VK_SUCCESS)

            swapChainFramebuffers += pFramebuffer.get(0)
        }
        stackPop()
    }

    fun cleanup() {
        for(framebuffer in swapChainFramebuffers) {
            vkDestroyFramebuffer(backend.logicalDevice.vkDevice, framebuffer, null)
        }

        for(imageView in swapChainImageViews) {
            vkDestroyImageView(backend.logicalDevice.vkDevice, imageView, null)
        }

        vkDestroySwapchainKHR(backend.logicalDevice.vkDevice, handle, null)
    }

    companion object {
        val logger = LoggerFactory.getLogger("client.vulkan")
    }
}