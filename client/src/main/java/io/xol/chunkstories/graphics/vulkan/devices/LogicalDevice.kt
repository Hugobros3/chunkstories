package io.xol.chunkstories.graphics.vulkan.devices

import io.xol.chunkstories.graphics.vulkan.CommandPool
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.resources.Cleanable
import io.xol.chunkstories.graphics.vulkan.util.ensureIs
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import org.slf4j.LoggerFactory
import java.util.concurrent.Semaphore
import kotlin.math.log

class LogicalDevice(val backend: VulkanGraphicsBackend, val physicalDevice: PhysicalDevice) {
    private val allQueues: List<Queue>

    lateinit var graphicsQueue: Queue private set
    lateinit var presentationQueue: Queue private set
    lateinit var transferQueue: Queue private set

    internal val handle: Long
    internal val vkDevice: VkDevice

    data class QueueRequest(val family: PhysicalDevice.QueueFamily, val target: (queue: Queue) -> Unit)

    init {
        logger.debug("Creating logical device")
        stackPush() // todo use use() when Contracts work correctly on AutoCloseable

        val graphicsQueueFamily = physicalDevice.queueFamilies.find { it.canGraphics }
                ?: throw Exception("Couldn't find an acceptable graphics queue family in $physicalDevice")
        val presentationQueueFamily = physicalDevice.queueFamilies.find { it.canPresent }
                ?: throw Exception("Couldn't find an acceptable presentation queue family in $physicalDevice")

        val transferQueueFamily = physicalDevice.queueFamilies.filter { it.canTransfer }.
                // we sort the list and give the existing graphics queue a weight, so we avoid selecting it if we have other options
                sortedBy { if(it == graphicsQueueFamily) 10 else 0 }.getOrNull(0)
                ?: throw Exception("Couldn't find an acceptable transfer queue family in $physicalDevice")

        val requests = listOf<QueueRequest>(
                QueueRequest(graphicsQueueFamily) { graphicsQueue = it },
                QueueRequest(presentationQueueFamily) { presentationQueue = it },
                QueueRequest(transferQueueFamily) { transferQueue = it }
        )
        logger.debug("Queues we would like to have: $requests")

        val mappedRequests = requests.groupBy { it.family }
        logger.debug("Queues of the same family merged together: $mappedRequests")

        val vkDeviceQueuesCreateInfo = VkDeviceQueueCreateInfo.callocStack(mappedRequests.size)
        var i = 0

        val actualRequestedQueueCounts = mappedRequests.map { (family, queues) ->
            val queues2create = Math.min(queues.size, family.maxInstances)
            if(queues2create < queues.size)
                logger.info("Max queueCount() of the queue family $family is under the requested amount of queues for that type (${queues.size}), queue aliasing will occur")

            vkDeviceQueuesCreateInfo.get(i++).sType(VK10.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO).apply {
                queueFamilyIndex(family.index)
                val floatBuffer = stackMallocFloat(1)
                floatBuffer.put(0, 1.0f)
                pQueuePriorities(floatBuffer)

                VkDeviceQueueCreateInfo.nqueueCount(address(), queues2create)
                //pQueuePriorities().put(0, 1.0f)
            }

            Pair(family, queues2create)
        }.toMap()

        // The queues we need depends if the two families are the same
        /*val vkDeviceQueuesCreateInfo: VkDeviceQueueCreateInfo.Buffer
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
        }*/

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

        var requestedExtensions = backend.requiredDeviceExtensions.toSet()
        requestedExtensions += "VK_KHR_get_memory_requirements2"

        if (backend.doNonUniformSamplerArrayAccess)
            requestedExtensions = setOf("VK_EXT_descriptor_indexing", "VK_KHR_maintenance3").union(requestedExtensions)

        val pRequiredExtensions = stackMallocPointer(requestedExtensions.size)
        requestedExtensions.forEachIndexed { i, e -> pRequiredExtensions.put(i, stackUTF8(e)) }

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

        /*if( graphicsQueueFamily == presentationQueueFamily ) {
            vkGetDeviceQueue(vkDevice, graphicsQueueFamily.index, 0, pQueue)
            graphicsQueue = Queue(VkQueue(pQueue.get(0), vkDevice), graphicsQueueFamily)
            presentationQueue = graphicsQueue
        } else {
            vkGetDeviceQueue(vkDevice, graphicsQueueFamily.index, 0, pQueue)
            graphicsQueue = Queue(VkQueue(pQueue.get(0), vkDevice), graphicsQueueFamily)

            vkGetDeviceQueue(vkDevice, presentationQueueFamily.index, 0, pQueue)
            presentationQueue = Queue(VkQueue(pQueue.get(0), vkDevice), graphicsQueueFamily)
        }*/

        //i = 0
        allQueues = mutableListOf()

        for ((family, queues) in mappedRequests) {
            var i = 0
            var queue: Queue? = null

            for (queueRequest in queues) {
                if(i < actualRequestedQueueCounts[family]!!) {
                    vkGetDeviceQueue(vkDevice, family.index, i++, pQueue)
                    queue = Queue(VkQueue(pQueue.get(0), vkDevice), family)
                    allQueues += queue
                }
                queueRequest.target.invoke(queue!!)
            }
        }

        stackPop()
        VulkanGraphicsBackend.logger.debug("Successfully created logical device $this")
    }

    fun cleanup() {
        allQueues

        vkDestroyDevice(vkDevice, null)
    }

    override fun toString(): String {
        return "LogicalDevice(handle=$handle, graphicsQueue=$graphicsQueue)"
    }

    inner class Queue(val handle: VkQueue, val family: PhysicalDevice.QueueFamily) {
        val mutex = Semaphore(1)

        val threadSafePools: ThreadLocal<CommandPool>
        private val allocatedThreadSafePools = mutableListOf<CommandPool>()

        init {
            threadSafePools = ThreadLocal.withInitial {
                val pool = CommandPool(backend, family, VK_COMMAND_POOL_CREATE_TRANSIENT_BIT or VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                allocatedThreadSafePools.add(pool)
                pool
            }
        }

        fun cleanup() {
            allocatedThreadSafePools.forEach(Cleanable::cleanup)
        }

        override fun toString(): String {
            return "Queue(handle=$handle, family=$family)"
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger("client.vulkan")
    }
}