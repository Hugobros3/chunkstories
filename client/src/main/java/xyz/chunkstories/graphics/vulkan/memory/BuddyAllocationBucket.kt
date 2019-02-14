package xyz.chunkstories.graphics.vulkan.memory

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkMemoryAllocateInfo
import org.lwjgl.vulkan.VkMemoryRequirements
import org.lwjgl.vulkan.VkMemoryType
import xyz.chunkstories.graphics.vulkan.resources.Cleanable
import xyz.chunkstories.graphics.vulkan.util.VkDeviceMemory
import xyz.chunkstories.graphics.vulkan.util.ensureIs
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class BuddyAllocationBucket(memoryManager: VulkanMemoryManager, memoryTypeIndex: Int, memoryType: VkMemoryType) : VulkanMemoryManager.Bucket(memoryManager, memoryTypeIndex, memoryType) {
    val allocatedBytesTotalAtomic = AtomicLong(0)
    override val allocatedBytesTotal: Long
        get() = allocatedBytesTotalAtomic.get()

    val maxOrder = 27 // 2^17 = 128kb 2^27 = 128mb
    val bucketLock = ReentrantLock()

    val blocks = mutableListOf<SharedAllocation>()
    val freeBuddies = Array(maxOrder + 1) { mutableListOf<SharedAllocation.Buddy>() }

    enum class BuddyState {
        FREE,
        IN_USE,
        SPLIT,
    }

    fun pow2(order: Int): Int {
        var a = 1
        for (i in 0 until order)
            a *= 2
        return a
    }

    init {
        println("lolwut")
    }

    inner class SharedAllocation(val firstBlock: Boolean) : Cleanable {
        val size = pow2(maxOrder).toLong()
        val deviceMemory: VkDeviceMemory

        val sharedLock = ReentrantLock()
        val mainBuddy = Buddy(this, maxOrder, 0, null)

        inner class Buddy(val alloc: SharedAllocation, val order: Int, override val offset: Long, val parent: Buddy?) : VulkanMemoryManager.Allocation() {
            override val size = pow2(order).toLong()

            override val lock: Lock
                get() = sharedLock
            override val deviceMemory: VkDeviceMemory
                get() = this@SharedAllocation.deviceMemory

            var state: BuddyState = BuddyState.FREE
            lateinit var buddy: Buddy

            override fun cleanup() {
                sharedLock.withLock {
                    this.state = BuddyState.FREE

                    //println("pre delete"+freeBuddies.toList())

                    when {
                        parent == null -> {
                            //println("is top level buddy, freeing block")
                            if (firstBlock)
                                freeBuddies[order].add(this)
                            else
                                alloc.cleanup()
                        }
                        buddy.state == BuddyState.FREE -> {
                            if(parent.state != BuddyState.SPLIT)
                                throw Exception("Assertion fail")

                            //println("buddy is free too, freeing parent")
                            freeBuddies[order].remove(this)
                            freeBuddies[order].remove(buddy)
                            parent.cleanup()

                        }
                        else -> {
                            freeBuddies[order].add(this)
                        }
                    }

                    //println("post delete"+freeBuddies.toList().withIndex())
                }
            }
        }

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
    }

    override fun allocateSlice(requirements: VkMemoryRequirements): VulkanMemoryManager.Allocation {
        bucketLock.withLock {
            var orderRequired = 0
            var sizeToAllocate = 1
            while(sizeToAllocate < requirements.size()) {
                orderRequired++
                sizeToAllocate *= 2
            }

            //println("Allocating order $orderRequired ($sizeToAllocate) for allocation exactly sized ${requirements.size()}")
            //println("pre alloc"+freeBuddies.toList().withIndex().map { (i, l) -> "$i: ${l.size}" })

            val buddyAvailableDirectly = freeBuddies[orderRequired].firstOrNull()
            if(buddyAvailableDirectly != null) {
                buddyAvailableDirectly.state = BuddyState.IN_USE
                freeBuddies[orderRequired].remove(buddyAvailableDirectly)
                return buddyAvailableDirectly
            }

            fun findBuddy2split() : SharedAllocation.Buddy? {
                // We'll look for the smallest buddy we can find
                for(order in orderRequired..maxOrder) {
                    //println("order $order would fit for $orderRequired, is it available? ${freeBuddies[order]}")
                    val available = freeBuddies[order].firstOrNull()
                    if(available != null) {
                        freeBuddies[available.order].remove(available)
                        //println("can split buddy $available (o=${available.order}")
                        return available
                    }
                }

                return null
            }

            fun newBuddy() : SharedAllocation.Buddy {
                val newAlloc = SharedAllocation(blocks.size == 0)
                blocks.add(newAlloc)
                //freeBuddies[maxOrder].add(newAlloc.mainBuddy)
                return newAlloc.mainBuddy
            }

            var buddyToSplit = findBuddy2split() ?: newBuddy()

            if(buddyToSplit.state != BuddyState.FREE)
                throw Exception("Assertion fail")

            val splitsToDo = buddyToSplit.order - orderRequired
            for(i in 0 until splitsToDo) {
                buddyToSplit.state = BuddyState.SPLIT

                with(buddyToSplit.alloc) {
                    val subBuddyA = Buddy(this, buddyToSplit.order - 1, buddyToSplit.offset, buddyToSplit)
                    val subBuddyB = Buddy(this, buddyToSplit.order - 1, buddyToSplit.offset + pow2(buddyToSplit.order - 1), buddyToSplit)
                    subBuddyA.buddy = subBuddyB
                    subBuddyB.buddy = subBuddyA

                    freeBuddies[subBuddyB.order].add(subBuddyB)
                    //println("made available buddy $subBuddyB order${subBuddyB.order} ${freeBuddies[subBuddyB.order]}")
                    buddyToSplit = subBuddyA
                }
            }

            val buddyToUse = buddyToSplit
            buddyToUse.state = BuddyState.IN_USE

            return buddyToUse
        }
    }

    override fun cleanup() {
        bucketLock.withLock {
            blocks.toList().forEach(Cleanable::cleanup)
        }
    }

    override fun toString(): String {
        return "BuddyAlloc(memoryType=${memoryTypeIndex} baseBlocks=${blocks.size} allocTotal=${allocatedBytesTotal / 1024}kb)"
    }
}

private fun Long.alignTo(alignment: Long): Long = when {
    alignment <= 1 -> this
    this % alignment == 0L -> this
    else -> this - (this % alignment) + alignment
}
