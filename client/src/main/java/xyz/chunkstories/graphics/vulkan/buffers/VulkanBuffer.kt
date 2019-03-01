package xyz.chunkstories.graphics.vulkan.buffers

import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkBufferCopy
import org.lwjgl.vulkan.VkBufferCreateInfo
import org.lwjgl.vulkan.VkMemoryRequirements
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.common.Cleanable
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
    private val deleteOnce = AtomicBoolean()
    private val allocation: VulkanMemoryManager.Allocation

    constructor(backend: VulkanGraphicsBackend, initialData: ByteBuffer, usageBits: Int, memoryUsage: MemoryUsagePattern) : this(backend, initialData.capacity().toLong(), usageBits, memoryUsage) {
        upload(initialData)
    }

    init {
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

        allocation = backend.memoryManager.allocateMemory(requirements, memoryUsage)
        allocation.lock.withLock {
            vkBindBufferMemory(backend.logicalDevice.vkDevice, handle, allocation.deviceMemory, allocation.offset)
        }
        stackPop()
    }

    /** Memory-maps the buffer and updates it */
    fun upload(dataToUpload: ByteBuffer) {
        if(dataToUpload.remaining() > bufferSize)
            throw Exception("This buffer does not have enough capacity (${dataToUpload.remaining()} > $bufferSize)")

        stackPush()

        if (memoryUsage.hostVisible) {
            allocation.lock.withLock {
                val ppData = stackMallocPointer(1)
                vkMapMemory(backend.logicalDevice.vkDevice, allocation.deviceMemory, allocation.offset, bufferSize, 0, ppData)

                val mappedMemory = ppData.getByteBuffer(bufferSize.toInt())
                mappedMemory.put(dataToUpload)

                vkUnmapMemory(backend.logicalDevice.vkDevice, allocation.deviceMemory)
            }
        } else {
            val pool = backend.logicalDevice.transferQueue.threadSafePools.get()
            val fence = backend.createFence(false)

            val stagingBuffer = VulkanBuffer(backend, bufferSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, MemoryUsagePattern.STAGING)
            stagingBuffer.upload(dataToUpload)

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

        stackPop()
    }

    override fun cleanup() {
        if (!deleteOnce.compareAndSet(false, true)) {
            Thread.dumpStack()
        }

        vkDestroyBuffer(backend.logicalDevice.vkDevice, handle, null)
        allocation.cleanup()
    }

    override fun toString(): String {
        return "VulkanBuffer(bufferSize=$bufferSize, memUsage=$memoryUsage, alloc=$allocation)"
    }
}