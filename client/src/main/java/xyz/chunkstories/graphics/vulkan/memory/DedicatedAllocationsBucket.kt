package xyz.chunkstories.graphics.vulkan.memory

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkMemoryAllocateInfo
import org.lwjgl.vulkan.VkMemoryRequirements
import org.lwjgl.vulkan.VkMemoryType
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.vulkan.util.VkDeviceMemory
import xyz.chunkstories.graphics.vulkan.util.ensureIs
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock

class DedicatedAllocationsBucket(memoryManager: VulkanMemoryManager, memoryTypeIndex: Int, memoryType: VkMemoryType) : VulkanMemoryManager.Bucket(memoryManager, memoryTypeIndex, memoryType) {
    override val stats: String
        get() = "${allocations.size} allocations, total ${allocatedBytesTotal/1024}kb"

    val allocatedBytesTotalAtomic = AtomicLong(0)
    override val allocatedBytesTotal: Long
        get() = allocatedBytesTotalAtomic.get()

    val allocations = mutableListOf<DedicatedSlice>()

    inner class DedicatedSlice(requirements: VkMemoryRequirements) : VulkanMemoryManager.Allocation() {

        override val lock = ReentrantLock()

        override val size = requirements.size()
        override val offset = 0L
        override val deviceMemory: VkDeviceMemory

        init {
            MemoryStack.stackPush()
            try {
                val pDeviceMemory = MemoryStack.stackMallocLong(1)
                val memoryAllocationInfo = VkMemoryAllocateInfo.callocStack().sType(VK10.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO).apply {
                    allocationSize(requirements.size())
                    memoryTypeIndex(memoryTypeIndex)
                }
                VK10.vkAllocateMemory(memoryManager.backend.logicalDevice.vkDevice, memoryAllocationInfo, null, pDeviceMemory).ensureIs("Failed to allocate memory !", VK10.VK_SUCCESS)
                deviceMemory = pDeviceMemory.get(0)
                allocatedBytesTotalAtomic.addAndGet(size)
            } finally {
                MemoryStack.stackPop()
            }
        }

        override fun cleanup() {
            VK10.vkFreeMemory(memoryManager.backend.logicalDevice.vkDevice, deviceMemory, null)
            allocatedBytesTotalAtomic.addAndGet(-size)
            allocations.remove(this)
        }
    }

    override fun allocateSlice(requirements: VkMemoryRequirements): VulkanMemoryManager.Allocation {
        val slice = DedicatedSlice(requirements)
        allocations.add(slice)
        return slice
    }

    override fun cleanup() {
        allocations.toList().forEach(Cleanable::cleanup)
    }

    override fun toString(): String {
        return "DedicatedAllocationsBucket(memoryType=${memoryTypeIndex} allocCount=${allocations.size} allocTotal=${allocatedBytesTotal/1024}kb)"
    }
}