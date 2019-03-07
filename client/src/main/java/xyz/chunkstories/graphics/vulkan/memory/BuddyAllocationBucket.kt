package xyz.chunkstories.graphics.vulkan.memory

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkMemoryAllocateInfo
import org.lwjgl.vulkan.VkMemoryRequirements
import org.lwjgl.vulkan.VkMemoryType
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.vulkan.util.VkDeviceMemory
import xyz.chunkstories.graphics.vulkan.util.ensureIs
import java.awt.image.BufferedImage
import java.io.File
import java.lang.Integer.max
import java.lang.Integer.min
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import javax.imageio.ImageIO
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

    inner class SharedAllocation(val firstBlock: Boolean) : Cleanable {
        val size = pow2(maxOrder).toLong()
        val deviceMemory: VkDeviceMemory

        val sharedLock = ReentrantLock()
        val mainBuddy = Buddy(this, maxOrder, 0, null)

        inner class Buddy(val alloc: SharedAllocation, val order: Int, override val offset: Long, val parent: Buddy?) : VulkanMemoryManager.Allocation() {
            override val size = pow2(order).toLong()

            override val lock: Lock
                get() = bucketLock
            override val deviceMemory: VkDeviceMemory
                get() = this@SharedAllocation.deviceMemory

            var state: BuddyState = BuddyState.FREE
            var usedSize = 0L
            lateinit var buddy: Buddy

            var left: Buddy? = null
            var right: Buddy? = null

            override fun cleanup() {
                bucketLock.withLock {
                    this.state = BuddyState.FREE
                    this.left = null
                    this.right = null

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
                buddyAvailableDirectly.usedSize = requirements.size()
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
                    buddyToSplit.left = subBuddyA
                    buddyToSplit.right = subBuddyB

                    freeBuddies[subBuddyB.order].add(subBuddyB)
                    //println("made available buddy $subBuddyB order${subBuddyB.order} ${freeBuddies[subBuddyB.order]}")
                    buddyToSplit = subBuddyA
                }
            }

            val buddyToUse = buddyToSplit
            buddyToUse.state = BuddyState.IN_USE
            buddyToUse.usedSize = requirements.size()

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

    fun exportAllocGraph(file: File) {
        val allocs = this.blocks.toList()
        val bufferedImage = BufferedImage( 1024,64 * allocs.size, BufferedImage.TYPE_4BYTE_ABGR )

        val grey = 0xFF808080
        val green = 0xFF00FF00
        val blue = 0xFF0000FF
        val red = 0xFFFF0000

        val scaler = bufferedImage.width / pow2(maxOrder).toDouble()
        for((i, alloc) in allocs.withIndex()) {
            bufferedImage.drawRect(0, i * 64, bufferedImage.width - 1, i * 64 + 48, grey.toInt())

            fun drawBuddy(buddy: SharedAllocation.Buddy) {
                val start = (buddy.offset * scaler).toInt()
                val end = ((buddy.offset + buddy.size) * scaler).toInt()
                when(buddy.state) {
                    BuddyState.FREE -> bufferedImage.drawRect(start, i * 64, end, i * 64 + 48, blue.toInt())
                    BuddyState.IN_USE -> {
                        val middle = ((buddy.offset + buddy.usedSize) * scaler).toInt()

                        bufferedImage.drawRect(start, i * 64, middle, i * 64 + 48, green.toInt())
                        if(buddy.usedSize < buddy.size)
                            bufferedImage.drawRect(middle, i * 64, end, i * 64 + 48, red.toInt())
                    }
                    BuddyState.SPLIT -> {
                        buddy.left?.let { drawBuddy(it) }
                        buddy.right?.let { drawBuddy(it) }
                    }
                }
            }

            drawBuddy(alloc.mainBuddy)
        }

        ImageIO.write(bufferedImage, "PNG", file)
        println("exported to $file")
    }

    override val stats: String
        get() {
            val allocs = this.blocks.toList()

            var used = 0L
            var wasted = 0L
            var free = 0L

            allocs.forEach { alloc ->

                fun countBuddy(buddy: SharedAllocation.Buddy) {
                    when(buddy.state) {
                        BuddyState.FREE -> free += buddy.size
                        BuddyState.IN_USE -> {
                            used += buddy.usedSize
                            wasted += buddy.size - buddy.usedSize
                        }
                        BuddyState.SPLIT -> {
                            buddy.left?.let { countBuddy(it) }
                            buddy.right?.let { countBuddy(it) }
                        }
                    }
                }

                countBuddy(alloc.mainBuddy)
            }

            return "#00FF00used ${used/1024/1024}mb #FFFFFF; #FF0000wasted ${wasted/1024/1024}mb #FFFFFF; #0000FFfree ${free/1024/1024}mb#FFFFFF"
        }
}

fun BufferedImage.drawRect(x0: Int, y0: Int, x1: Int, y1: Int, rgb: Int) {
    val x0 = max(0, x0)
    val y0 = max(0, y0)
    val x1 = min(this.width - 1, x1)
    val y1 = min(this.height - 1, y1)
    for(x in x0..x1)
        for(y in y0..y1)
            this.setRGB(x, y, rgb)
}
