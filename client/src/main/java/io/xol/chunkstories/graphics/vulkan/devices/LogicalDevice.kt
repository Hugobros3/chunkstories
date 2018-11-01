package io.xol.chunkstories.graphics.vulkan.devices

import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.util.ensureIs
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import org.slf4j.LoggerFactory

class LogicalDevice(val backend: VulkanGraphicsBackend, val physicalDevice: PhysicalDevice) {
    val graphicsQueue: Queue
    val presentationQueue: Queue

    internal val handle: Long
    internal val vkDevice : VkDevice

    init {
        logger.debug("Creating logical device")
        stackPush() // todo use use() when Contracts work correctly on AutoCloseable

        val graphicsQueueFamily = physicalDevice.queueFamilies.find { it.canGraphics } ?: throw Exception("Couldn't find an acceptable graphics queue family in $physicalDevice")
        val presentationQueueFamily = physicalDevice.queueFamilies.find { it.canPresent } ?: throw Exception("Couldn't find an acceptable presentation queue family in $physicalDevice")

        // The queues we need depends if the two families are the same
        val vkDeviceQueuesCreateInfo: VkDeviceQueueCreateInfo.Buffer
        if( graphicsQueueFamily == presentationQueueFamily ) {
            logger.debug("Note : Graphics and presentation queue families are the same. Creating a single queue !")

            vkDeviceQueuesCreateInfo = VkDeviceQueueCreateInfo.callocStack(1)
            vkDeviceQueuesCreateInfo.get(0).sType(VK10.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO).apply {
                queueFamilyIndex(graphicsQueueFamily.index)
                val floatBuffer = stackMallocFloat(1)
                floatBuffer.put(0, 1.0f)
                pQueuePriorities(floatBuffer)
            }

        } else {
            logger.debug("Note : Graphics and presentation queue families are different.")

            vkDeviceQueuesCreateInfo = VkDeviceQueueCreateInfo.callocStack(2)
            vkDeviceQueuesCreateInfo.get(0).sType(VK10.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO).apply {
                queueFamilyIndex(graphicsQueueFamily.index)
                val floatBuffer = stackMallocFloat(1)
                floatBuffer.put(0, 1.0f)
                pQueuePriorities(floatBuffer)
            }

            vkDeviceQueuesCreateInfo.get(1).sType(VK10.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO).apply {
                queueFamilyIndex(presentationQueueFamily.index)
                val floatBuffer = stackMallocFloat(1)
                floatBuffer.put(0, 1.0f)
                pQueuePriorities(floatBuffer)
            }
        }

        // The features we need
        val vkPhysicalDeviceFeatures = VkPhysicalDeviceFeatures.callocStack()
        vkPhysicalDeviceFeatures.shaderSampledImageArrayDynamicIndexing(true)

        // The layers we need
        var requestedLayers: PointerBuffer? = null
        if (backend.enableValidation) {
            requestedLayers = stackCallocPointer(1)
            requestedLayers.put(stackUTF8("VK_LAYER_LUNARG_standard_validation"))
            requestedLayers.flip()
        }

        val pRequiredExtensions = stackMallocPointer(backend.requiredDeviceExtensions.size)
        backend.requiredDeviceExtensions.forEachIndexed { i, e -> pRequiredExtensions.put(i, stackUTF8(e)) }

        val vkDeviceCreateInfo = VkDeviceCreateInfo.callocStack().sType(VK10.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO).apply {
            pQueueCreateInfos(vkDeviceQueuesCreateInfo)
            pEnabledFeatures(vkPhysicalDeviceFeatures)
            ppEnabledExtensionNames(pRequiredExtensions)
            ppEnabledLayerNames(requestedLayers)
        }

        val pDevice = stackMallocPointer(1)
        vkCreateDevice(physicalDevice.vkPhysicalDevice, vkDeviceCreateInfo, null, pDevice).ensureIs("Failed to create device from $physicalDevice", VK10.VK_SUCCESS)
        handle = pDevice.get(0)
        vkDevice = VkDevice(handle, physicalDevice.vkPhysicalDevice, vkDeviceCreateInfo)

        val pQueue = stackMallocPointer(1)

        if( graphicsQueueFamily == presentationQueueFamily ) {
            vkGetDeviceQueue(vkDevice, graphicsQueueFamily.index, 0, pQueue)
            graphicsQueue = Queue(VkQueue(pQueue.get(0), vkDevice), graphicsQueueFamily)
            presentationQueue = graphicsQueue
        } else {
            vkGetDeviceQueue(vkDevice, graphicsQueueFamily.index, 0, pQueue)
            graphicsQueue = Queue(VkQueue(pQueue.get(0), vkDevice), graphicsQueueFamily)

            vkGetDeviceQueue(vkDevice, presentationQueueFamily.index, 0, pQueue)
            presentationQueue = Queue(VkQueue(pQueue.get(0), vkDevice), graphicsQueueFamily)
        }

        stackPop()
        VulkanGraphicsBackend.logger.debug("Successfully created logical device $this")
    }

    fun cleanup() {
        vkDestroyDevice(vkDevice, null)
    }

    override fun toString(): String {
        return "LogicalDevice(handle=$handle, graphicsQueue=$graphicsQueue)"
    }

    inner class Queue(val handle: VkQueue, val family: PhysicalDevice.QueueFamily) {
        override fun toString(): String {
            return "Queue(handle=$handle, family=$family)"
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger("client.vulkan")
    }
}