package xyz.chunkstories.graphics.vulkan.memory

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkMemoryAllocateInfo
import org.lwjgl.vulkan.VkMemoryRequirements
import org.lwjgl.vulkan.VkMemoryType
import xyz.chunkstories.graphics.vulkan.resources.Cleanable
import xyz.chunkstories.graphics.vulkan.util.VkDeviceMemory
import xyz.chunkstories.graphics.vulkan.util.ensureIs
import xyz.chunkstories.gui.logger
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class StaticBucket(memoryManager: VulkanMemoryManager, memoryTypeIndex: Int, memoryType: VkMemoryType) : VulkanMemoryManager.Bucket(memoryManager, memoryTypeIndex, memoryType) {
    override val stats: String
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    val allocatedBytesTotalAtomic = AtomicLong(0)
    override val allocatedBytesTotal: Long
        get() = allocatedBytesTotalAtomic.get()

    val defaultAllocationSize = 128 * MB
    val bucketLock = ReentrantLock()

    val blocks = mutableListOf<SharedAllocation>()

    inner class SharedAllocation(val size: Long, val firstBlock: Boolean) : Cleanable {
        val deviceMemory: VkDeviceMemory

        val sharedLock = ReentrantLock()

        var offset = 0L
        val suballocations = mutableListOf<SubAllocation>()

        init {
            MemoryStack.stackPush()
            try {
                val pDeviceMemory = MemoryStack.stackMallocLong(1)
                val memoryAllocationInfo = VkMemoryAllocateInfo.callocStack().sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO).apply {
                    allocationSize(size)
                    memoryTypeIndex(memoryTypeIndex)
                }
                vkAllocateMemory(memoryManager.backend.logicalDevice.vkDevice, memoryAllocationInfo, null, pDeviceMemory).ensureIs("Failed to allocate memory !", VK_SUCCESS)
                deviceMemory = pDeviceMemory.get(0)
                allocatedBytesTotalAtomic.addAndGet(size)
            } finally {
                MemoryStack.stackPop()
            }
        }

        override fun cleanup() {
            bucketLock.withLock {
                vkFreeMemory(memoryManager.backend.logicalDevice.vkDevice, deviceMemory, null)
                allocatedBytesTotalAtomic.addAndGet(-size)
                blocks.remove(this)
            }
        }

        fun subAllocate(requirements: VkMemoryRequirements) : SubAllocation {
            val alignedOffset = offset.alignTo(requirements.alignment())
            offset = alignedOffset + requirements.size()
            val suballoc = SubAllocation(alignedOffset, requirements.size())
            suballocations.add(suballoc)
            return suballoc
        }

        inner class SubAllocation(override val offset: Long, override val size: Long) : VulkanMemoryManager.Allocation() {
            override val lock: Lock
                get() = sharedLock

            override val deviceMemory: VkDeviceMemory
                get() = this@SharedAllocation.deviceMemory

            //val stackTrace = Thread.currentThread().stackTrace

            override fun cleanup() {
                sharedLock.withLock {
                    suballocations.remove(this)
                    if(suballocations.size == 0) {
                        /*logger.warn("Cleaning block of bucket${this@StaticBucket}")
                        Thread.dumpStack()
                        println(blocks[0].suballocations)*/
                        this@SharedAllocation.cleanup()
                    }
                }
            }

            override fun toString(): String {
                return "SubAllocation(offset=$offset, size=$size)"
            }
        }
    }

    override fun allocateSlice(requirements: VkMemoryRequirements): VulkanMemoryManager.Allocation {
        bucketLock.withLock {
            var goodBlock: SharedAllocation? = null
            for(block in blocks) {
                val alignedOffset = block.offset.alignTo(requirements.alignment())
                val remainingSpace = block.size - alignedOffset
                if (remainingSpace >= requirements.size()) {
                    goodBlock = block
                    break
                }
                println("block $block remSpace = $remainingSpace < ${requirements.size()}")
            }

            if (goodBlock == null) {
                goodBlock = SharedAllocation(defaultAllocationSize, blocks.size == 0)
                blocks.add(goodBlock)
            }

            return goodBlock.subAllocate(requirements)
        }
    }

    override fun cleanup() {
        bucketLock.withLock {
            blocks.toList().forEach(Cleanable::cleanup)
        }
    }

    override fun toString(): String {
        return "StaticBucket(memoryType=${memoryTypeIndex} blocksCount=${blocks.size} allocTotal=${allocatedBytesTotal/1024}kb)"
    }
}

private fun Long.alignTo(alignment: Long): Long = when {
    alignment <= 1 -> this
    this % alignment == 0L -> this
    else -> this - (this % alignment) + alignment
}
