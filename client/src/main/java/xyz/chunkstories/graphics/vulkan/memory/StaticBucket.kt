package xyz.chunkstories.graphics.vulkan.memory

import org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT
import org.lwjgl.vulkan.VkMemoryRequirements
import org.lwjgl.vulkan.VkMemoryType
import java.util.concurrent.locks.ReentrantLock

class StaticBucket(memoryManager: VulkanMemoryManager, memoryTypeIndex: Int, memoryType: VkMemoryType) : VulkanMemoryManager.Bucket(memoryManager, memoryTypeIndex, memoryType) {
    override val allocatedBytesTotal: Long
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    val defaultAllocationSize = 128 * MB
    val requiredFlags = VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT
    val lock = ReentrantLock()

    init {
    }

    override fun allocateSlice(requirements: VkMemoryRequirements): VulkanMemoryManager.Allocation {
        val requiredTypes = requirements.memoryTypeBits()
        TODO()
    }

    override fun cleanup() {

    }
}