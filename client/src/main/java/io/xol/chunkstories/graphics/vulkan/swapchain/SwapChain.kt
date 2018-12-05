package io.xol.chunkstories.graphics.vulkan.swapchain

import io.xol.chunkstories.graphics.vulkan.*
import io.xol.chunkstories.graphics.vulkan.util.*
import org.lwjgl.glfw.GLFW.glfwWaitEvents
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR
import org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK10.*
import org.slf4j.LoggerFactory

class SwapChain(val backend: VulkanGraphicsBackend, displayRenderPass: VkRenderPass, oldSwapChain: SwapChain?) {

    val handle: Long
    val swapChainImages: List<VkImage>
    val swapChainImageViews: List<VkImageView>

    lateinit var swapChainFramebuffers: List<VkFramebuffer>
        private set
    internal var imagesCount: Int
        private set

    internal var maxFramesInFlight: Int = -1
        private set
    private var inflightFrameIndex = 0

    val performanceCounter = VulkanPerformanceCounter(this)

    // We keep a reference to old frames to manage their data lifecycle, even past GPU execution
    private lateinit var inFlightFrames: Array<Frame?>

    // Those sync primitives get recycled accross multiple frames
    private lateinit var imageAvailableSemaphores: List<VkSemaphore>
    private lateinit var renderingSemaphores: List<VkSemaphore>
    private lateinit var inFlightFences: List<VkFence>

    var expired = false

    init {
        logger.debug("Creating swapchain...")
        stackPush()

        imagesCount = backend.physicalDevice.swapchainDetails.imageCount.first + 1
        if (imagesCount > backend.physicalDevice.swapchainDetails.imageCount.last)
            imagesCount = backend.physicalDevice.swapchainDetails.imageCount.last
        logger.debug("Asking for $imagesCount in the swapchain")

        val vkSwapchainCreateInfoKHR = VkSwapchainCreateInfoKHR.callocStack().sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR).apply {
            surface(backend.surface.handle)
            minImageCount(imagesCount)
            imageFormat(backend.physicalDevice.swapchainDetails.formatToUse.ordinal)
            imageColorSpace(backend.physicalDevice.swapchainDetails.colorSpaceToUse)
            imageExtent().width(backend.window.width)
            imageExtent().height(backend.window.height)

            logger.debug("Using presentation mode ${backend.physicalDevice.swapchainDetails.presentationModeToUse}")
            presentMode(backend.physicalDevice.swapchainDetails.presentationModeToUse.ordinal)
            //imageExtent(backend.physicalDevice.swapchainDetails.swapExtentToUse)
            imageArrayLayers(1)
            //TODO maybe not needed once we do everything in offscreen buffers
            imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT or VK_IMAGE_USAGE_TRANSFER_DST_BIT)

            preTransform(backend.physicalDevice.swapchainDetails.transformToUse)
            compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
            clipped(true)
            oldSwapchain(oldSwapChain?.handle ?: VK_NULL_HANDLE)
            //oldSwapchain( VK_NULL_HANDLE )
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
        for (image in swapChainImages) {
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

        createFramebuffers(displayRenderPass)
        createSemaphores()

        stackPop()
    }

    private fun createFramebuffers(displayRenderPass: VkRenderPass) {
        stackPush()
        swapChainFramebuffers = mutableListOf()

        for (imageView in swapChainImageViews) {
            val pAttachment = stackMallocLong(1)
            pAttachment.put(0, imageView)

            val framebufferCreateInfo = VkFramebufferCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO).apply {
                renderPass(displayRenderPass)
                pAttachments(pAttachment)
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

    //TODO make it an option
    fun getMaxFramesInFlight() = imagesCount

    private fun createSemaphores() {
        maxFramesInFlight = getMaxFramesInFlight()

        imageAvailableSemaphores = List(maxFramesInFlight) { backend.createSemaphore() }
        renderingSemaphores = List(maxFramesInFlight) { backend.createSemaphore() }
        inFlightFences = List(maxFramesInFlight) { backend.createFence(true) }
        inFlightFrames = arrayOfNulls<Frame?>(maxFramesInFlight)
    }

    fun beginFrame(frameNumber: Int): Frame {
        val retiringFrame = inFlightFrames[inflightFrameIndex]

        stackPush()

        val currentInflightFrameIndex = inflightFrameIndex

        val fence = inFlightFences[currentInflightFrameIndex]
        vkWaitForFences(backend.logicalDevice.vkDevice, fence, true, Long.MAX_VALUE)

        if(retiringFrame != null) {
            retiringFrame.recyclingTasks.forEach { it.invoke() }
            //performanceCounter.whenFrameEnds(retiringFrame)
        }

        vkResetFences(backend.logicalDevice.vkDevice, fence)

        val imageAvailableSemaphore = imageAvailableSemaphores[currentInflightFrameIndex]
        val renderingFinishedSemaphore = renderingSemaphores[currentInflightFrameIndex]

        val pImageIndex = stackMallocInt(1)
        val result = vkAcquireNextImageKHR(backend.logicalDevice.vkDevice, handle, Long.MAX_VALUE, imageAvailableSemaphore, VK_NULL_HANDLE, pImageIndex)

        if (result == VK_ERROR_OUT_OF_DATE_KHR || result == VK_SUBOPTIMAL_KHR || this.expired) {
            logger.debug("Recreating swap chain !")
            vkDeviceWaitIdle(backend.logicalDevice.vkDevice)
            while (true) {
                stackPush()
                // Querying that makes the validation layer happy when resizing, so whatever
                val pSurfaceCapabilities = VkSurfaceCapabilitiesKHR.callocStack()
                vkGetPhysicalDeviceSurfaceCapabilitiesKHR(backend.physicalDevice.vkPhysicalDevice, backend.surface.handle, pSurfaceCapabilities)

                if (backend.window.width == 0 || backend.window.height == 0)
                    logger.debug("Was minized, waiting to become a workable size again")
                else if (backend.window.width > pSurfaceCapabilities.maxImageExtent().width() || backend.window.height > pSurfaceCapabilities.maxImageExtent().height())
                    logger.debug("Weird condition, game window is exceeding the max extent of the surface, waiting until conditions change...")
                else {
                    stackPop()
                    break
                }
                stackPop()

                glfwWaitEvents()
            }

            val newSwapchain = SwapChain(backend, backend.renderToBackbuffer, this)
            backend.swapchain = newSwapchain
            backend.recreateSwapchainDependencies()
            cleanup()

            stackPop()
            return newSwapchain.beginFrame(frameNumber)
        }

        val swapchainImageIndex = pImageIndex.get(0)

        stackPop()

        val frame =  Frame(frameNumber, swapchainImageIndex, swapChainImages[swapchainImageIndex], swapChainImageViews[swapchainImageIndex], swapChainFramebuffers[swapchainImageIndex], currentInflightFrameIndex, imageAvailableSemaphore, renderingFinishedSemaphore, fence, System.nanoTime())
        performanceCounter.whenFrameBegins(frame)

        inFlightFrames[inflightFrameIndex] = frame
        return frame
    }

    fun finishFrame(frame: Frame) {
        stackPush()

        val presentInfo = VkPresentInfoKHR.callocStack().sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR).apply {
            val waitSemaphores = stackMallocLong(1)
            waitSemaphores.put(0, frame.renderFinishedSemaphore)
            pWaitSemaphores(waitSemaphores)

            val swapChains = stackMallocLong(1)
            swapChains.put(0, handle)
            pSwapchains(swapChains)
            swapchainCount(1)

            pImageIndices(stackInts(frame.swapchainImageIndex))
            pResults(null)
        }

        backend.logicalDevice.presentationQueue.mutex.acquireUninterruptibly()
        vkQueuePresentKHR(backend.logicalDevice.presentationQueue.handle, presentInfo)
        backend.logicalDevice.presentationQueue.mutex.release()

        inflightFrameIndex = (inflightFrameIndex + 1) % maxFramesInFlight
        stackPop()
    }

    fun cleanup() {
        inFlightFences.forEach { vkDestroyFence(backend.logicalDevice.vkDevice, it, null) }
        imageAvailableSemaphores.forEach { vkDestroySemaphore(backend.logicalDevice.vkDevice, it, null) }
        renderingSemaphores.forEach { vkDestroySemaphore(backend.logicalDevice.vkDevice, it, null) }

        for (framebuffer in swapChainFramebuffers) {
            vkDestroyFramebuffer(backend.logicalDevice.vkDevice, framebuffer, null)
        }

        for (imageView in swapChainImageViews) {
            vkDestroyImageView(backend.logicalDevice.vkDevice, imageView, null)
        }

        vkDestroySwapchainKHR(backend.logicalDevice.vkDevice, handle, null)
    }

    companion object {
        val logger = LoggerFactory.getLogger("client.vulkan")
    }
}