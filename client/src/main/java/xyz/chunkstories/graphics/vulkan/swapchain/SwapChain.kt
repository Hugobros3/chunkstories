package xyz.chunkstories.graphics.vulkan.swapchain

import xyz.chunkstories.graphics.vulkan.*
import xyz.chunkstories.graphics.vulkan.util.*
import org.lwjgl.glfw.GLFW.glfwWaitEvents
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR
import org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK10.*
import org.slf4j.LoggerFactory
import xyz.chunkstories.graphics.common.util.getAnimationTime
import xyz.chunkstories.graphics.vulkan.resources.InflightFrameResource

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

    val performanceCounter = PerformanceCounter()

    // We keep a reference to old frames to manage their data lifecycle, even past GPU execution
    private lateinit var inFlightFrames: Array<VulkanFrame?>

    // Those sync primitives get recycled accross multiple frames
    private lateinit var imageAvailableSemaphores: List<VkSemaphore>
    private lateinit var renderingSemaphores: List<VkSemaphore>
    private lateinit var inFlightFences: List<VkFence>

    lateinit var lastFrame: VulkanFrame private set

    var expired = false

    /** We want to automatically resize the resources that are unique per inflight-frame */
    internal val listeners = mutableListOf<InflightFrameResource<*>>()

    //TODO make it an option
    fun getMaxFramesInFlight() = imagesCount

    init {
        val presentationMode = backend.physicalDevice.swapchainDetails.presentationModeToUse

        logger.debug("Creating swapchain using $presentationMode ...")
        stackPush()

        imagesCount = when (presentationMode) {
            PresentationMode.IMMEDIATE -> 2
            PresentationMode.MAILBOX -> 3
            PresentationMode.FIFO -> 1
            PresentationMode.FIFO_RELAXED -> 2
        }

        if (imagesCount > backend.physicalDevice.swapchainDetails.imageCount.last)
            imagesCount = backend.physicalDevice.swapchainDetails.imageCount.last
        if (imagesCount < backend.physicalDevice.swapchainDetails.imageCount.first)
            imagesCount = backend.physicalDevice.swapchainDetails.imageCount.first
        logger.debug("Asking for a swapchain with $imagesCount images")

        // Create the views & framebuffers
        val vkSwapchainCreateInfoKHR = VkSwapchainCreateInfoKHR.callocStack().sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR).apply {
            surface(backend.surface.handle)
            minImageCount(imagesCount)
            imageFormat(backend.physicalDevice.swapchainDetails.formatToUse.ordinal)
            imageColorSpace(backend.physicalDevice.swapchainDetails.colorSpaceToUse)
            imageExtent().width(backend.window.width)
            imageExtent().height(backend.window.height)

            logger.debug("Using presentation mode ${backend.physicalDevice.swapchainDetails.presentationModeToUse}")
            presentMode(presentationMode.ordinal)
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

        if (backend.logicalDevice.graphicsQueue.family == backend.logicalDevice.presentationQueue.family) {
            vkSwapchainCreateInfoKHR.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE)
        } else {
            vkSwapchainCreateInfoKHR.imageSharingMode(VK_SHARING_MODE_CONCURRENT)


            // using a set here avoids providing duplicates of the queues
            val queuesSet = setOf(backend.logicalDevice.graphicsQueue.family.index, backend.logicalDevice.presentationQueue.family.index)

            val pIndices = stackMallocInt(queuesSet.size)
            pIndices.put(queuesSet.toIntArray())
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

        if(oldSwapChain != null) {
            listeners.addAll(oldSwapChain.listeners)
            listeners.forEach { it.whenSwapchainSizeChanges(imagesCount) }
        }
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

    private fun createSemaphores() {
        maxFramesInFlight = getMaxFramesInFlight()

        imageAvailableSemaphores = List(maxFramesInFlight) { backend.createSemaphore() }
        renderingSemaphores = List(maxFramesInFlight) { backend.createSemaphore() }
        inFlightFences = List(maxFramesInFlight) { backend.createFence(true) }
        inFlightFrames = arrayOfNulls<VulkanFrame?>(maxFramesInFlight)
    }

    fun beginFrame(frameNumber: Int): VulkanFrame {
        // Are we retiring some older frame ? ( most often we do, except at initialization time )
        val retiringFrame = inFlightFrames[inflightFrameIndex]

        stackPush()

        val fence = inFlightFences[inflightFrameIndex]
        vkWaitForFences(backend.logicalDevice.vkDevice, fence, true, Long.MAX_VALUE)

        if(retiringFrame != null) {
            retiringFrame.recyclingTasks.forEach { it.invoke() }
            // Null out this index so we don't accidentally refer to the old frame again
            inFlightFrames[inflightFrameIndex] = null

            //performanceCounter.whenFrameEnds(retiringFrame)
        }

        vkResetFences(backend.logicalDevice.vkDevice, fence)

        val imageAvailableSemaphore = imageAvailableSemaphores[inflightFrameIndex]
        val renderingFinishedSemaphore = renderingSemaphores[inflightFrameIndex]

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
                    logger.debug("Was minimized, waiting to become a workable size again")
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

        val frame = VulkanFrame(frameNumber, getAnimationTime().toFloat(), swapchainImageIndex, swapChainImages[swapchainImageIndex], swapChainImageViews[swapchainImageIndex], swapChainFramebuffers[swapchainImageIndex], imageAvailableSemaphore, renderingFinishedSemaphore, fence, System.nanoTime())
        performanceCounter.whenFrameBegins()
        lastFrame = frame

        inFlightFrames[inflightFrameIndex] = frame
        return frame
    }

    fun finishFrame(frame: VulkanFrame) {
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

    fun flush() {
        // Finish the recycling tasks for the frames that were in-flight
        for(i in 0 until maxFramesInFlight) {
            inFlightFrames[i]?.recyclingTasks?.forEach { it.invoke() }
            inFlightFrames[i] = null
        }
    }

    fun cleanup() {
        flush()

        inFlightFences.forEach { vkDestroyFence(backend.logicalDevice.vkDevice, it, null) }
        imageAvailableSemaphores.forEach { vkDestroySemaphore(backend.logicalDevice.vkDevice, it, null) }
        renderingSemaphores.forEach { vkDestroySemaphore(backend.logicalDevice.vkDevice, it, null) }

        for (framebuffer in swapChainFramebuffers) {
            vkDestroyFramebuffer(backend.logicalDevice.vkDevice, framebuffer, null)
        }

        for (imageView in swapChainImageViews) {
            vkDestroyImageView(backend.logicalDevice.vkDevice, imageView, null)
        }

        // We don't destroy the images because the swap chain owns them.

        vkDestroySwapchainKHR(backend.logicalDevice.vkDevice, handle, null)
    }

    companion object {
        val logger = LoggerFactory.getLogger("client.vulkan")
    }
}