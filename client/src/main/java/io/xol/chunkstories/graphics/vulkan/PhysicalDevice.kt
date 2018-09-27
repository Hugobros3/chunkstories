package io.xol.chunkstories.graphics.vulkan

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.VK10.*
import org.slf4j.LoggerFactory

class PhysicalDevice(private val backend: VulkanGraphicsBackend, internal val vkPhysicalDevice: VkPhysicalDevice) {
    val deviceName: String
    val deviceType: PhysicalDeviceType
    val deviceId: Int

    internal val suitable: Boolean
    internal var fitnessScore: Int

    val queueFamilies: List<QueueFamily>

    val availableExtensions: List<String>

    var swapchainDetails: PhysicalDevice.SwapChainSupportDetails
        internal set

    init {
        MemoryStack.stackPush() // todo use use() when Contracts work correctly on AutoCloseable

        // Query device properties
        val vkPhysicalDeviceProperties = VkPhysicalDeviceProperties.callocStack()
        VK10.vkGetPhysicalDeviceProperties(vkPhysicalDevice, vkPhysicalDeviceProperties)

        deviceName = vkPhysicalDeviceProperties.deviceNameString()
        deviceType = vkPhysicalDeviceProperties.deviceType().physicalDeviceType()
        deviceId = vkPhysicalDeviceProperties.deviceID()

        val vkPhysicalDeviceMemoryProperties = VkPhysicalDeviceMemoryProperties.callocStack()
        VK10.vkGetPhysicalDeviceMemoryProperties(vkPhysicalDevice, vkPhysicalDeviceMemoryProperties)

        // Query device features
        val vkPhysicalDeviceFeatures = VkPhysicalDeviceFeatures.callocStack()
        VK10.vkGetPhysicalDeviceFeatures(vkPhysicalDevice, vkPhysicalDeviceFeatures)

        // Query device extensions
        val pExtensionsCount = stackMallocInt(1)
        vkEnumerateDeviceExtensionProperties(vkPhysicalDevice, null as? CharSequence, pExtensionsCount, null).ensureIs("Failed to obtain extensions count",VK_SUCCESS)
        val pExtensionsProperties = VkExtensionProperties.callocStack(pExtensionsCount.get(0))
        vkEnumerateDeviceExtensionProperties(vkPhysicalDevice, null as? CharSequence, pExtensionsCount, pExtensionsProperties).ensureIs("Failed to enumerate extensions", VK_SUCCESS, VK_INCOMPLETE)

        availableExtensions = mutableListOf()
        for(extension in pExtensionsProperties) {
            availableExtensions += extension.extensionNameString()
        }

        logger.debug("$availableExtensions")

        // Query queue families properties
        val pQueueFamilyCount = MemoryStack.stackMallocInt(1)
        VK10.vkGetPhysicalDeviceQueueFamilyProperties(vkPhysicalDevice, pQueueFamilyCount, null)
        val queueFamiliesProperties = VkQueueFamilyProperties.callocStack(pQueueFamilyCount.get(0))
        VK10.vkGetPhysicalDeviceQueueFamilyProperties(vkPhysicalDevice, pQueueFamilyCount, queueFamiliesProperties)

        queueFamilies = mutableListOf()
        for ((index, queueFamilyProperties) in queueFamiliesProperties.withIndex()) {
            val canGraphics = queueFamilyProperties.queueFlags() and VK10.VK_QUEUE_GRAPHICS_BIT != 0
            val canCompute = queueFamilyProperties.queueFlags() and VK10.VK_QUEUE_COMPUTE_BIT != 0
            val canTransfer = queueFamilyProperties.queueFlags() and VK10.VK_QUEUE_TRANSFER_BIT != 0

            val pPresentSupport = stackMallocInt(1)
            vkGetPhysicalDeviceSurfaceSupportKHR(vkPhysicalDevice, index, backend.surface.handle, pPresentSupport).ensureIs("Couldn't query support for presentation", VK_SUCCESS)
            val canPresent = pPresentSupport.get(0) > 0

            queueFamilies += QueueFamily(index, canGraphics, canCompute, canTransfer, canPresent)
        }

        // Query swapchain details
        val pSurfaceCapabilities = VkSurfaceCapabilitiesKHR.callocStack()
        vkGetPhysicalDeviceSurfaceCapabilitiesKHR(vkPhysicalDevice, backend.surface.handle, pSurfaceCapabilities)

        val pFormatsCount = stackMallocInt(1)
        vkGetPhysicalDeviceSurfaceFormatsKHR(vkPhysicalDevice, backend.surface.handle, pFormatsCount, null)
        val pSurfaceFormats = VkSurfaceFormatKHR.callocStack(pFormatsCount.get(0))
        vkGetPhysicalDeviceSurfaceFormatsKHR(vkPhysicalDevice, backend.surface.handle, pFormatsCount, pSurfaceFormats)

        val pPresentModeCount = stackMallocInt(1)
        vkGetPhysicalDeviceSurfacePresentModesKHR(vkPhysicalDevice, backend.surface.handle, pPresentModeCount, null)
        val pPresentModes = stackMallocInt(pPresentModeCount.get(0))
        vkGetPhysicalDeviceSurfacePresentModesKHR(vkPhysicalDevice, backend.surface.handle, pPresentModeCount, pPresentModes)
        val pPresentModesIa = IntArray(pPresentModeCount.get(0))
        pPresentModes.get(pPresentModesIa, 0, pPresentModes.capacity())

        swapchainDetails = SwapChainSupportDetails(pSurfaceCapabilities, pSurfaceFormats, pPresentModesIa.map { it.presentationMode() } )

        // Decide if suitable or not based on all that
        suitable = vkPhysicalDeviceFeatures.geometryShader() && availableExtensions.containsAll(backend.requiredDeviceExtensions) && swapchainDetails.suitable
        fitnessScore = 1 + deviceType.fitnessScoreBonus

        MemoryStack.stackPop()
    }

    inner class QueueFamily(val index: Int, val canGraphics: Boolean, val canCompute: Boolean, val canTransfer: Boolean, val canPresent: Boolean) {
        override fun toString() = "QueueFamily(index=$index, canGraphics=$canGraphics, canCompute=$canCompute, canTransfer=$canTransfer, canPresent=$canPresent)"
    }

    inner class SwapChainSupportDetails(capabilities: VkSurfaceCapabilitiesKHR, surfaceFormats: VkSurfaceFormatKHR.Buffer, val presentationModes: List<PresentionMode>) {
        val suitable : Boolean

        val formatToUse : Formats
        val colorSpaceToUse = VK_COLOR_SPACE_SRGB_NONLINEAR_KHR
        val presentationModeToUse: PresentionMode
        val swapExtentToUse: VkExtent2D

        init {
            suitable = surfaceFormats.capacity() > 0 && presentationModes.isNotEmpty()

            logger.debug("Listing formats")
            for(format in surfaceFormats) {
                logger.debug("Format: ${Formats.values()[format.format()].name} colorspace : ${format.colorSpace()}")
            }

            // Chose a surface format
            // TODO HDR support and way smarter selection
            val availableFormats = surfaceFormats.map { Formats.values()[it.format()] }

            if(surfaceFormats.capacity() == 1 && surfaceFormats.get(0).format() == VK_FORMAT_UNDEFINED)
                formatToUse = Formats.VK_FORMAT_R8G8B8A8_UNORM
            else {
                val preferredFormat = Formats.VK_FORMAT_R8G8B8A8_UNORM
                if(availableFormats.contains(preferredFormat))
                    formatToUse = preferredFormat
                else
                    formatToUse = availableFormats[0]
            }

            // Chose a presentation mode
            val preferredPresentationModes = when(backend.window.client.configuration.getValue("client.graphics.syncMode")) {
                "vsync" -> listOf(PresentionMode.FIFO)
                "fastest" -> listOf(PresentionMode.IMMEDIATE, PresentionMode.MAILBOX, PresentionMode.FIFO_RELAXED, PresentionMode.FIFO)
                else -> listOf(PresentionMode.MAILBOX, PresentionMode.FIFO)
            }

            var bestCompromise : PresentionMode? = null
            for(mostLikedPresentationMode in preferredPresentationModes) {
                if(presentationModes.contains(mostLikedPresentationMode)) {
                    bestCompromise = mostLikedPresentationMode
                }
            }

            presentationModeToUse = bestCompromise ?: throw Exception("Could not find a compromise between the presentation mode the user wanted and those available")

            // Look I'm not interested in this swap extent bs
            // TODO maybe later
            swapExtentToUse = capabilities.currentExtent()
            if(swapExtentToUse.width() == Int.MAX_VALUE)
                throw Exception("Not willing to deal with this nonsense right now")
        }
    }

    override fun toString(): String {
        return "PhysicalDevice(deviceName='$deviceName', deviceType=$deviceType, queueFamilies=$queueFamilies)"
    }

    companion object {
        val logger = LoggerFactory.getLogger("client.vulkan")
    }
}