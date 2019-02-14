package xyz.chunkstories.graphics.vulkan.memory

import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkMemoryRequirements
import org.lwjgl.vulkan.VkMemoryType
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.devices.LogicalDevice
import xyz.chunkstories.graphics.vulkan.resources.Cleanable
import xyz.chunkstories.graphics.vulkan.util.VkDeviceMemory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

enum class MemoryUsagePattern(val hostVisible: Boolean) {
    /** For resources that you load once and (barely) ever deallocate, like textures. Can be linearly allocated */
    STATIC(false),
    /** For resources that you'll use for a certain time then discard, like meshes or map data. These need managed fragmentation. */
    SEMI_STATIC(false),
    /** For resources that get uploaded to every frame, typically uniform buffers and instance data */
    DYNAMIC(true),
    /** For resources that are used once to upload to a static copy */
    STAGING(true)
}

const val KB = 1024L
const val MB = 1024 * KB

class VulkanMemoryManager(val backend: VulkanGraphicsBackend, val device: LogicalDevice) : Cleanable {

    val vkPhysicalDeviceMemoryProperties: VkPhysicalDeviceMemoryProperties = VkPhysicalDeviceMemoryProperties.malloc()
    val buckets = mutableMapOf<Int, MutableMap<MemoryUsagePattern, Bucket>>()
    val bucketsLock = ReentrantLock()

    val fastBucketAccess = ConcurrentHashMap<Long, Bucket>()
    val allocatedBytesTotal: Long
    get() {
        var total = 0L
        for(a in buckets.values)
            for(b in a.values)
                total += b.allocatedBytesTotal

        return total
    }

    init {
        vkGetPhysicalDeviceMemoryProperties(backend.physicalDevice.vkPhysicalDevice, vkPhysicalDeviceMemoryProperties)
    }

    private fun findMemoryTypeToUse(acceptableMemoryTypesBitfield: Int, memoryPropertiesFlags: Int): Pair<Int, VkMemoryType> {
        for (i in 0 until vkPhysicalDeviceMemoryProperties.memoryTypeCount()) {
            val memoryType = vkPhysicalDeviceMemoryProperties.memoryTypes(i)
            // each bit in memoryTypeBits refers to an acceptable memory type, via it's index in the memoryTypes list of deviceMemoryProperties
            // it's rather confusing at first. We just have to shift the index and AND it with the requirements bits to know if the type is suitable
            if (acceptableMemoryTypesBitfield and (1 shl i) != 0) {
                // we check that memory type has all the flags we need too
                if ((memoryType.propertyFlags() and memoryPropertiesFlags) == memoryPropertiesFlags) {
                    return Pair(i, memoryType)
                }
            }
        }

        throw Exception("Unsatisfiable condition: Can't find an appropriate memory type suiting both buffer requirements and usage requirements")
    }

    fun allocateMemory(requirements: VkMemoryRequirements, usagePattern: MemoryUsagePattern): Allocation {
        // Find bucket to use
        val bucketHashId = (requirements.memoryTypeBits().toLong() shl 32) or usagePattern.ordinal.toLong()

        return bucketsLock.withLock {
            fastBucketAccess.getOrPut(bucketHashId) {
                val acceptableMemoryTypesBitfield = requirements.memoryTypeBits()
                var acceptableBucket: Bucket? = null

                // Check if we got an existing bucket matching the usage pattern
                // and one of the acceptable memory types
                for (memoryTypeIndex in 0 until vkPhysicalDeviceMemoryProperties.memoryTypeCount()) {
                    // each bit in acceptableMemoryTypesBitfield refers to an acceptable memory type, via it's index in the memoryTypes list of deviceMemoryProperties
                    // it's rather confusing at first. We just have to shift the index and AND it with the requirements bits to know if the type is suitable
                    if (acceptableMemoryTypesBitfield and (1 shl memoryTypeIndex) != 0) {
                        val bucket = buckets[memoryTypeIndex]?.get(usagePattern)

                        if (bucket != null)
                            acceptableBucket = bucket
                    }
                }

                if (acceptableBucket != null) {
                    fastBucketAccess[bucketHashId] = acceptableBucket
                    return@getOrPut acceptableBucket
                }

                val newBucket = createBucketForTypeAndUsagePattern(acceptableMemoryTypesBitfield, usagePattern)
                buckets.getOrPut(newBucket.memoryTypeIndex) { mutableMapOf() }[usagePattern] = newBucket
                fastBucketAccess[bucketHashId] = newBucket
                return@getOrPut newBucket
            }
        }.allocateSlice(requirements)
    }

    private fun createBucketForTypeAndUsagePattern(acceptableMemoryTypesBitfield: Int, usagePattern: MemoryUsagePattern): Bucket = when (usagePattern) {
        MemoryUsagePattern.STATIC -> {
            val (memoryTypeIndex, memoryType) = findMemoryTypeToUse(acceptableMemoryTypesBitfield, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT)
            BuddyAllocationBucket(this, memoryTypeIndex, memoryType)
        }
        MemoryUsagePattern.SEMI_STATIC -> {
            val (memoryTypeIndex, memoryType) = findMemoryTypeToUse(acceptableMemoryTypesBitfield, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT)
            BuddyAllocationBucket(this, memoryTypeIndex, memoryType)
        }
        MemoryUsagePattern.DYNAMIC -> {
            val (memoryTypeIndex, memoryType) = findMemoryTypeToUse(acceptableMemoryTypesBitfield, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT)
            BuddyAllocationBucket(this, memoryTypeIndex, memoryType)
        }
        MemoryUsagePattern.STAGING -> {
            val (memoryTypeIndex, memoryType) = findMemoryTypeToUse(acceptableMemoryTypesBitfield, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT)
            BuddyAllocationBucket(this, memoryTypeIndex, memoryType)
        }
    }

    abstract class Bucket(val memoryManager: VulkanMemoryManager, val memoryTypeIndex: Int, val memoryType: VkMemoryType) : Cleanable {
        abstract val allocatedBytesTotal: Long

        abstract fun allocateSlice(requirements: VkMemoryRequirements): Allocation
    }

    abstract class Allocation : Cleanable {
        abstract val lock : Lock
        abstract val deviceMemory: VkDeviceMemory
        abstract val offset: Long
        abstract val size: Long
    }

    override fun cleanup() {
        buckets.values.forEach { it.values.forEach(Cleanable::cleanup) }
        vkPhysicalDeviceMemoryProperties.free()
    }
}