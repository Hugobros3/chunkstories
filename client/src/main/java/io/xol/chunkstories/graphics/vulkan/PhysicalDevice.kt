package io.xol.chunkstories.graphics.vulkan

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackMallocInt
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.VK10.*
import org.slf4j.LoggerFactory
import java.nio.IntBuffer

class PhysicalDevice(private val backend: VulkanGraphicsBackend, internal val vkPhysicalDevice: VkPhysicalDevice) {
    val deviceName: String
    val deviceType: PhysicalDeviceType
    val deviceId: Int

    internal val suitable: Boolean
    internal var fitnessScore: Int

    val queueFamilies: List<QueueFamily>

    val availableExtensions: List<String>

    internal val swapchainDetails: PhysicalDevice.SwapChainSupportDetails

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
        vkEnumerateDeviceExtensionProperties(vkPhysicalDevice, null as? CharSequence, pExtensionsCount, null).ensureIs("Failed to obtain extensions count", VK_SUCCESS)
        val pExtensionsProperties = VkExtensionProperties.callocStack(pExtensionsCount.get(0))
        vkEnumerateDeviceExtensionProperties(vkPhysicalDevice, null as? CharSequence, pExtensionsCount, pExtensionsProperties).ensureIs("Failed to enumerate extensions", VK_SUCCESS, VK_INCOMPLETE)

        availableExtensions = mutableListOf()
        for (extension in pExtensionsProperties) {
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

        swapchainDetails = SwapChainSupportDetails(pSurfaceCapabilities, pSurfaceFormats, pPresentModes)

        // Decide if suitable or not based on all that
        suitable = vkPhysicalDeviceFeatures.geometryShader() && availableExtensions.containsAll(backend.requiredDeviceExtensions) && swapchainDetails.suitable
        fitnessScore = 1 + deviceType.fitnessScoreBonus

        MemoryStack.stackPop()
    }

    inner class QueueFamily(val index: Int, val canGraphics: Boolean, val canCompute: Boolean, val canTransfer: Boolean, val canPresent: Boolean) {
        override fun toString() = "QueueFamily(index=$index, canGraphics=$canGraphics, canCompute=$canCompute, canTransfer=$canTransfer, canPresent=$canPresent)"
    }

    inner class SwapChainSupportDetails(capabilities: VkSurfaceCapabilitiesKHR, surfaceFormats: VkSurfaceFormatKHR.Buffer, pPresentModes: IntBuffer) {
        val suitable: Boolean

        val availableFormats: List<Formats>
        val availablePresentationModes: List<PresentationMode>
        val imageCount: IntRange

        val transformToUse : Int
        val formatToUse: Formats
            get() =
                if (availableFormats == listOf(Formats.VK_FORMAT_UNDEFINED))
                    Formats.VK_FORMAT_R8G8B8A8_UNORM
                else {
                    val preferredFormat = Formats.VK_FORMAT_R8G8B8A8_UNORM
                    if (availableFormats.contains(preferredFormat))
                        preferredFormat
                    else
                        availableFormats[0]
                }

        val presentationModeToUse: PresentationMode
            get() {
                val preferredPresentationModes = when (backend.window.client.configuration.getValue("client.graphics.syncMode")) {
                    "vsync" -> listOf(PresentationMode.FIFO)
                    "fastest" -> listOf(PresentationMode.IMMEDIATE, PresentationMode.MAILBOX, PresentationMode.FIFO_RELAXED, PresentationMode.FIFO)
                    else -> listOf(PresentationMode.MAILBOX, PresentationMode.FIFO)
                }

                var bestCompromise: PresentationMode? = null
                for (mostLikedPresentationMode in preferredPresentationModes) {
                    if (availablePresentationModes.contains(mostLikedPresentationMode)) {
                        bestCompromise = mostLikedPresentationMode
                    }
                }

                return bestCompromise ?: throw Exception("Could not find a compromise between the presentation mode the user wanted and those available")
            }

        val swapExtentToUse: VkExtent2D
        /** Afaik only supported color space for now, so whatever */
        val colorSpaceToUse = VK_COLOR_SPACE_SRGB_NONLINEAR_KHR

        init {
            availableFormats = surfaceFormats.map { Formats.values()[it.format()] }

            val pPresentModesIa = IntArray(pPresentModes.capacity())
            pPresentModes.get(pPresentModesIa, 0, pPresentModes.capacity())
            availablePresentationModes = pPresentModesIa.map { it.presentationMode() }

            // Look I'm not interested in this swap extent bs
            // TODO maybe later
            swapExtentToUse = capabilities.currentExtent()
            if (swapExtentToUse.width() == Int.MAX_VALUE)
                throw Exception("Not willing to deal with this nonsense right now")

            imageCount = if(capabilities.maxImageCount() != 0) capabilities.minImageCount()..capabilities.maxImageCount() else capabilities.minImageCount()..Int.MAX_VALUE

            transformToUse = capabilities.currentTransform()

            suitable = surfaceFormats.capacity() > 0 && availablePresentationModes.isNotEmpty()
        }


    }

    override fun toString(): String {
        return "PhysicalDevice(deviceName='$deviceName', deviceType=$deviceType, queueFamilies=$queueFamilies)"
    }

    companion object {
        val logger = LoggerFactory.getLogger("client.vulkan")
    }
}