package xyz.chunkstories.graphics.vulkan.devices

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackMallocInt
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.EXTDescriptorIndexing.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DESCRIPTOR_INDEXING_FEATURES_EXT
import org.lwjgl.vulkan.KHRGetPhysicalDeviceProperties2.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2_KHR
import org.lwjgl.vulkan.KHRGetPhysicalDeviceProperties2.vkGetPhysicalDeviceFeatures2KHR
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.VK10.*
import org.slf4j.LoggerFactory
import xyz.chunkstories.client.InternalClientOptions
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.util.*
import java.nio.IntBuffer

class PhysicalDevice(private val backend: VulkanGraphicsBackend, internal val vkPhysicalDevice: VkPhysicalDevice) {
    val deviceName: String
    val deviceType: PhysicalDeviceType
    val deviceId: Int

    val maxBoundSets: Int

    /** I don't think I really care about GS tbh */
    val canDoGS: Boolean

    internal val suitable: Boolean
    internal var fitnessScore: Int

    val queueFamilies: List<QueueFamily>

    val availableExtensions: List<String>
    val texturesArrayIndexingSupportTier: TexturesArrayIndexingSupportTier

    internal val swapchainDetails: SwapChainSupportDetails

    init {
        MemoryStack.stackPush() // todo use use() when Contracts work correctly on AutoCloseable

        // Query device properties
        val vkPhysicalDeviceProperties = VkPhysicalDeviceProperties.callocStack()
        vkGetPhysicalDeviceProperties(vkPhysicalDevice, vkPhysicalDeviceProperties)

        deviceName = vkPhysicalDeviceProperties.deviceNameString()
        deviceType = vkPhysicalDeviceProperties.deviceType().physicalDeviceType()
        deviceId = vkPhysicalDeviceProperties.deviceID()

        // Query device limits
        maxBoundSets = vkPhysicalDeviceProperties.limits().maxBoundDescriptorSets()

        // Query device extensions
        val pExtensionsCount = stackMallocInt(1)
        vkEnumerateDeviceExtensionProperties(vkPhysicalDevice, null as? CharSequence, pExtensionsCount, null).ensureIs("Failed to obtain extensions count", VK_SUCCESS)
        val pExtensionsProperties = VkExtensionProperties.callocStack(pExtensionsCount.get(0))
        vkEnumerateDeviceExtensionProperties(vkPhysicalDevice, null as? CharSequence, pExtensionsCount, pExtensionsProperties).ensureIs("Failed to enumerate extensions", VK_SUCCESS, VK_INCOMPLETE)

        availableExtensions = mutableListOf()
        for (extension in pExtensionsProperties) {
            availableExtensions += extension.extensionNameString()
        }

        logger.debug("Available Vulkan extensions on $deviceName: $availableExtensions")

        // Query device features
        val vkPhysicalDeviceFeatures2 = VkPhysicalDeviceFeatures2.callocStack().sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2_KHR)
        var descriptorIndexingFeatures: VkPhysicalDeviceDescriptorIndexingFeaturesEXT? = null
        if (availableExtensions.contains("VK_EXT_descriptor_indexing")) {
            descriptorIndexingFeatures = VkPhysicalDeviceDescriptorIndexingFeaturesEXT.callocStack().sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DESCRIPTOR_INDEXING_FEATURES_EXT)
            vkPhysicalDeviceFeatures2.pNext(descriptorIndexingFeatures.address())
        }
        vkGetPhysicalDeviceFeatures2KHR(vkPhysicalDevice, vkPhysicalDeviceFeatures2)

        canDoGS = vkPhysicalDeviceFeatures2.features().geometryShader()

        texturesArrayIndexingSupportTier = when {
            // No shaderSampledImageArrayDynamicIndexing !? This is ancient crap, how did this even get a vk driver ?
            // Well, swiftshader doesn't emulate this yet ...
            !vkPhysicalDeviceFeatures2.features().shaderSampledImageArrayDynamicIndexing() -> TexturesArrayIndexingSupportTier.TIER_0

            // Best-case scenario: we have a capable descriptor indexing implementation !
            vkPhysicalDeviceFeatures2.features().shaderSampledImageArrayDynamicIndexing() &&
                    descriptorIndexingFeatures != null &&
                    descriptorIndexingFeatures.shaderSampledImageArrayNonUniformIndexing() &&
                    descriptorIndexingFeatures.descriptorBindingVariableDescriptorCount() &&
                    descriptorIndexingFeatures.descriptorBindingSampledImageUpdateAfterBind() &&
                    descriptorIndexingFeatures.descriptorBindingPartiallyBound() &&
                    descriptorIndexingFeatures.runtimeDescriptorArray() -> TexturesArrayIndexingSupportTier.TIER_2

            // Anything else is considered tier 1
            else -> TexturesArrayIndexingSupportTier.TIER_1
        }

        // Query queue families properties
        val pQueueFamilyCount = MemoryStack.stackMallocInt(1)
        vkGetPhysicalDeviceQueueFamilyProperties(vkPhysicalDevice, pQueueFamilyCount, null)
        val queueFamiliesProperties = VkQueueFamilyProperties.callocStack(pQueueFamilyCount.get(0))
        vkGetPhysicalDeviceQueueFamilyProperties(vkPhysicalDevice, pQueueFamilyCount, queueFamiliesProperties)

        queueFamilies = mutableListOf()
        for ((index, queueFamilyProperties) in queueFamiliesProperties.withIndex()) {
            val canGraphics = queueFamilyProperties.queueFlags() and VK10.VK_QUEUE_GRAPHICS_BIT != 0
            val canCompute = queueFamilyProperties.queueFlags() and VK10.VK_QUEUE_COMPUTE_BIT != 0
            val canTransfer = queueFamilyProperties.queueFlags() and VK10.VK_QUEUE_TRANSFER_BIT != 0

            val pPresentSupport = stackMallocInt(1)
            vkGetPhysicalDeviceSurfaceSupportKHR(vkPhysicalDevice, index, backend.surface.handle, pPresentSupport).ensureIs("Couldn't query support for presentation", VK_SUCCESS)
            val canPresent = pPresentSupport.get(0) > 0

            queueFamilies += QueueFamily(index, queueFamiliesProperties.queueCount(), canGraphics, canCompute, canTransfer, canPresent)
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

        // Look for diverging descriptor access capability

        // Decide if suitable or not based on all that
        suitable = availableExtensions.containsAll(backend.requiredDeviceExtensions) && swapchainDetails.suitable
        fitnessScore = 1 + deviceType.fitnessScoreBonus

        MemoryStack.stackPop()
    }

    inner class QueueFamily(val index: Int, val maxInstances: Int, val canGraphics: Boolean, val canCompute: Boolean, val canTransfer: Boolean, val canPresent: Boolean) {
        override fun toString() = "QueueFamily(index=$index, maxInstances=$maxInstances, canGraphics=$canGraphics, canCompute=$canCompute, canTransfer=$canTransfer, canPresent=$canPresent)"
    }

    inner class SwapChainSupportDetails(capabilities: VkSurfaceCapabilitiesKHR, surfaceFormats: VkSurfaceFormatKHR.Buffer, pPresentModes: IntBuffer) {
        val suitable: Boolean

        val availableFormats: List<VulkanFormat>
        val availablePresentationModes: List<PresentationMode>
        val imageCount: IntRange

        val transformToUse: Int
        val formatToUse: VulkanFormat
            get() {
                if (availableFormats == listOf(VulkanFormat.VK_FORMAT_UNDEFINED))
                    return VulkanFormat.VK_FORMAT_R8G8B8A8_UNORM
                else {
                    val preferredFormats = listOf(VulkanFormat.VK_FORMAT_R8G8B8A8_UNORM, VulkanFormat.VK_FORMAT_B8G8R8A8_UNORM)

                    for (preferredFormat in preferredFormats)
                        if (availableFormats.contains(preferredFormat))
                            return preferredFormat

                    return availableFormats[0]
                }
            }

        val presentationModeToUse: PresentationMode
            get() {
                val preferredPresentationModes = when (backend.window.client.configuration.getValue(InternalClientOptions.syncMode)) {
                    "vsync" -> listOf(PresentationMode.FIFO)
                    "tripleBuffering" -> listOf(PresentationMode.MAILBOX, PresentationMode.FIFO)
                    else -> listOf(PresentationMode.IMMEDIATE, PresentationMode.MAILBOX, PresentationMode.FIFO_RELAXED, PresentationMode.FIFO)
                }

                //var bestCompromise: PresentationMode? = null
                for (mostLikedPresentationMode in preferredPresentationModes) {
                    if (availablePresentationModes.contains(mostLikedPresentationMode)) {
                        return mostLikedPresentationMode
                    }
                }

                throw Exception("Could not find a compromise between the presentation mode the user wanted and those available")
            }

        private val swapExtentToUse: VkExtent2D
        /** Afaik only supported color space for now, so whatever */
        val colorSpaceToUse = VK_COLOR_SPACE_SRGB_NONLINEAR_KHR

        init {
            availableFormats = surfaceFormats.map { VulkanFormat.values()[it.format()] }

            val pPresentModesIa = IntArray(pPresentModes.capacity())
            pPresentModes.get(pPresentModesIa, 0, pPresentModes.capacity())
            availablePresentationModes = pPresentModesIa.map { it.presentationMode() }

            // Look I'm not interested in this swap extent bs
            // TODO maybe later
            swapExtentToUse = capabilities.currentExtent()
            if (swapExtentToUse.width() == Int.MAX_VALUE)
                throw Exception("Not willing to deal with this nonsense right now")

            imageCount = if (capabilities.maxImageCount() != 0) capabilities.minImageCount()..capabilities.maxImageCount() else capabilities.minImageCount()..Int.MAX_VALUE

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

/** The capability of texture array indexing support.
 * See http://chunkstories.xyz/blog/a-note-on-descriptor-indexing/ */
enum class TexturesArrayIndexingSupportTier {
    TIER_0,
    TIER_1,
    TIER_2
}
