package xyz.chunkstories.graphics.vulkan.buffers

import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkBufferCopy
import org.lwjgl.vulkan.VkBufferCreateInfo
import org.lwjgl.vulkan.VkMemoryRequirements
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.resources.Cleanable
import xyz.chunkstories.graphics.vulkan.memory.MemoryUsagePattern
import xyz.chunkstories.graphics.vulkan.memory.VulkanMemoryManager
import xyz.chunkstories.graphics.vulkan.util.VkBuffer
import xyz.chunkstories.graphics.vulkan.util.createFence
import xyz.chunkstories.graphics.vulkan.util.ensureIs
import xyz.chunkstories.graphics.vulkan.util.waitFence
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.withLock

open class VulkanBuffer(val backend: VulkanGraphicsBackend, val bufferSize: Long, bufferUsageBits: Int, val memoryUsage: MemoryUsagePattern) : Cleanable {
    val handle: VkBuffer
    //val allocation: Long
    //val memoryType: Int
    // private val allocatedMemory: VkDeviceMemory

    constructor(backend: VulkanGraphicsBackend, initialData: ByteBuffer, usageBits: Int, memoryUsage: MemoryUsagePattern) : this(backend, initialData.capacity().toLong(), usageBits, memoryUsage) {
        upload(initialData)
    }

    private var allocation: VulkanMemoryManager.Allocation

    init {
        /*VmaAllocator.allocatedBytes.addAndGet(bufferSize )
        VmaAllocator.allocations.incrementAndGet()
        backend.vmaAllocator.lock.lock()*/
        stackPush()
        val bufferInfo = VkBufferCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO).apply {
            size(bufferSize)

            when(memoryUsage) {
                MemoryUsagePattern.STATIC, MemoryUsagePattern.SEMI_STATIC -> usage(bufferUsageBits or VK_BUFFER_USAGE_TRANSFER_DST_BIT)
                MemoryUsagePattern.DYNAMIC -> usage(bufferUsageBits)
                MemoryUsagePattern.STAGING -> usage(bufferUsageBits) //TODO this could just be transfer src ?
            }

            sharingMode(VK_SHARING_MODE_EXCLUSIVE)
            flags(0)
        }

        val pBuffer = stackMallocLong(1)
        vkCreateBuffer(backend.logicalDevice.vkDevice, bufferInfo, null, pBuffer).ensureIs("Failed to create buffer!", VK_SUCCESS)
        handle = pBuffer.get(0)

        val requirements = VkMemoryRequirements.callocStack()
        vkGetBufferMemoryRequirements(backend.logicalDevice.vkDevice, handle, requirements)

        /*val requiredFlags = if (hostVisible) {
            /*VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT or */VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
        } else
            VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT

        allocation = backend.memoryManager.allocateMemoryGivenRequirements(requirements, requiredFlags)*/
        allocation = backend.memoryManager.allocateMemory(requirements, memoryUsage)
        allocation.lock.withLock {
            vkBindBufferMemory(backend.logicalDevice.vkDevice, handle, allocation.deviceMemory, allocation.offset)
        }

        /*val vmaAllocCreateInfo = VmaAllocationCreateInfo.callocStack().apply {
            //usage(VMA_MEMORY_USAGE_GPU_ONLY)
            if(hostVisible) {
                requiredFlags(/*VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT or */VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT)
            } else
                requiredFlags(VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT)
        }

        val pBuffer = stackMallocLong(1)
        val pAllocation = stackMallocPointer(1)

        val allocationInfo = VmaAllocationInfo.calloc()
        vmaCreateBuffer(backend.vmaAllocator.handle, bufferInfo, vmaAllocCreateInfo, pBuffer, pAllocation, allocationInfo).ensureIs("VMA: failed to allocate vram :(", VK_SUCCESS)
        memoryType = allocationInfo.memoryType()
        handle = pBuffer.get(0)
        allocation = pAllocation.get(0)

        backend.vmaAllocator.lock.unlock()*/
        stackPop()
    }

    /** Memory-maps the buffer and updates it */
    fun upload(data: ByteBuffer) {
        assert(data.remaining() <= bufferSize)

        backend.vmaAllocator.lock.lock()
        val pushed = stackPush()

        //if(Thread.currentThread().name.startsWith("Main"))
        //    println("${Thread.currentThread().name} stack size ${pushed.frameIndex} + $hostVisible ${pushed.pointer}")

        if (memoryUsage.hostVisible) {
            allocation.lock.withLock {
                val ppData = stackMallocPointer(1)
                //vmaMapMemory(backend.vmaAllocator.handle, allocation, ppData).ensureIs("VMA: Failed to map memory", VK_SUCCESS)
                vkMapMemory(backend.logicalDevice.vkDevice, allocation.deviceMemory, allocation.offset, bufferSize, 0, ppData)

                val mappedMemory = ppData.getByteBuffer(bufferSize.toInt())
                mappedMemory.put(data)

                //vmaUnmapMemory(backend.vmaAllocator.handle, allocation)
                vkUnmapMemory(backend.logicalDevice.vkDevice, allocation.deviceMemory)
            }
        } else {
            val pool = backend.logicalDevice.transferQueue.threadSafePools.get()

            val fence = backend.createFence(false)

            val stagingBuffer = VulkanBuffer(backend, bufferSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, MemoryUsagePattern.STAGING)
            stagingBuffer.upload(data)

            val commandBuffer = pool.createOneUseCB()
            val region = VkBufferCopy.callocStack(1).apply {
                size(bufferSize)
                dstOffset(0)
                srcOffset(0)
            }
            vkCmdCopyBuffer(commandBuffer, stagingBuffer.handle, handle, region)

            pool.submitOneTimeCB(commandBuffer, backend.logicalDevice.transferQueue, fence)

            backend.waitFence(fence)

            vkDestroyFence(backend.logicalDevice.vkDevice, fence, null)
            vkFreeCommandBuffers(backend.logicalDevice.vkDevice, pool.handle, commandBuffer)
            stagingBuffer.cleanup()
        }

        backend.vmaAllocator.lock.unlock()
        stackPop()
    }

    val deleteOnce = AtomicBoolean()

    override fun cleanup() {
        if (!deleteOnce.compareAndSet(false, true)) {
            Thread.dumpStack()
            println("cleaned buffer TWICE wtf")
        }

        vkDestroyBuffer(backend.logicalDevice.vkDevice, handle, null)
        allocation.cleanup()

        /*VmaAllocator.allocatedBytes.addAndGet(-(bufferSize))
        VmaAllocator.allocations.decrementAndGet()

        backend.vmaAllocator.lock.lock()
        vmaDestroyBuffer(backend.vmaAllocator.handle, handle, allocation)
        backend.vmaAllocator.lock.unlock()*/
    }

    override fun toString(): String {
        return "VulkanBuffer(bufferSize=$bufferSize, memUsage=$memoryUsage, alloc=$allocation)"
    }
}