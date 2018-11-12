package io.xol.chunkstories.graphics.vulkan.buffers

import io.xol.chunkstories.graphics.vulkan.util.VkBuffer
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.resources.Cleanable
import io.xol.chunkstories.graphics.vulkan.util.createFence
import io.xol.chunkstories.graphics.vulkan.util.ensureIs
import io.xol.chunkstories.graphics.vulkan.util.waitFence
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.util.vma.Vma.*
import org.lwjgl.util.vma.VmaAllocationCreateInfo
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkBufferCopy
import org.lwjgl.vulkan.VkBufferCreateInfo
import org.lwjgl.vulkan.VkFenceCreateInfo
import java.nio.ByteBuffer

open class VulkanBuffer(val backend: VulkanGraphicsBackend, val bufferSize: Long, usageBits: Int, val hostVisible: Boolean) : Cleanable {
    val handle: VkBuffer
    val allocation: Long
    // private val allocatedMemory: VkDeviceMemory

    constructor(backend: VulkanGraphicsBackend, initialData: ByteBuffer, usageBits: Int) : this(backend, initialData.capacity().toLong(), usageBits, false) {
        upload(initialData)
    }

    init {
        stackPush()
        val bufferInfo = VkBufferCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO).apply {
            size(bufferSize)

            if(hostVisible)
                usage(usageBits)
            else
                usage(usageBits or VK_BUFFER_USAGE_TRANSFER_DST_BIT)

            sharingMode(VK_SHARING_MODE_EXCLUSIVE)
            flags(0)
        }

        val vmaAllocCreateInfo = VmaAllocationCreateInfo.callocStack().apply {
            //usage(VMA_MEMORY_USAGE_GPU_ONLY)
            if(hostVisible) {
                requiredFlags(VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT or VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT)
            } else
                requiredFlags(VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT)
        }

        val pBuffer = stackMallocLong(1)
        //vmaAllocation = VmaAllocationInfo.calloc()
        val pAllocation = stackPointers(1)

        vmaCreateBuffer(backend.vmaAllocator.handle, bufferInfo, vmaAllocCreateInfo, pBuffer, pAllocation, null).ensureIs("VMA: failed to allocate vram :(", VK_SUCCESS)
        handle = pBuffer.get(0)
        allocation = pAllocation.get(0)

        //val allocation = VmaAllocation(pAllocation)

        /*val pBuffer = stackMallocLong(1)
        vkCreateBuffer(backend.logicalDevice.vkDevice, bufferInfo, null, pBuffer)
        handle = pBuffer.get(0)

        val memoryRequirements = VkMemoryRequirements.callocStack()
        vkGetBufferMemoryRequirements(backend.logicalDevice.vkDevice, handle, memoryRequirements)

        allocatedMemory = backend.memoryManager.allocateMemoryGivenRequirements(memoryRequirements,  VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT)

        vkBindBufferMemory(backend.logicalDevice.vkDevice, handle, allocatedMemory, 0)*/

        stackPop()
    }

    /** Memory-maps the buffer and updates it */
    fun upload(data: ByteBuffer) {
        assert(data.remaining() <= bufferSize)

        stackPush()

        if(hostVisible) {
            val ppData = stackMallocPointer(1)
            vmaMapMemory(backend.vmaAllocator.handle, allocation, ppData).ensureIs("VMA: Failed to map memory", VK_SUCCESS)
            //vkMapMemory(backend.logicalDevice.vkDevice, allocatedMemory, 0, bufferSize, 0, ppData)

            val mappedMemory = ppData.getByteBuffer(bufferSize.toInt())
            mappedMemory.put(data)

            vmaUnmapMemory(backend.vmaAllocator.handle, allocation)
            //vkUnmapMemory(backend.logicalDevice.vkDevice, allocatedMemory)
        } else {
            val pool = backend.threadSafePools.get()

            /*val fenceCreateInfo = VkFenceCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO).apply {
                this.flags(0)
            }
            val pFence = stackMallocLong(1)

            vkCreateFence(backend.logicalDevice.vkDevice, fenceCreateInfo, null, pFence).ensureIs("Failed to create upload fence :/", VK_SUCCESS)
            val fence = pFence.get(0)*/
            val fence = backend.createFence(false)

            val stagingBuffer = VulkanBuffer(backend, bufferSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, true)
            stagingBuffer.upload(data)

            val commandBuffer = pool.createOneUseCB()
            val region = VkBufferCopy.callocStack(1).apply {
                size(bufferSize)
                dstOffset(0)
                srcOffset(0)
            }
            vkCmdCopyBuffer(commandBuffer, stagingBuffer.handle, handle, region)

            pool.submitOneTimeCB(commandBuffer, backend.logicalDevice.graphicsQueue, fence)

            backend.waitFence(fence)

            vkDestroyFence(backend.logicalDevice.vkDevice, fence, null)
            vkFreeCommandBuffers(backend.logicalDevice.vkDevice, pool.handle, commandBuffer)
            stagingBuffer.cleanup()
        }

        stackPop()
    }

    override fun cleanup() {
        vmaDestroyBuffer(backend.vmaAllocator.handle, handle, allocation)
        // vkDestroyBuffer(backend.logicalDevice.vkDevice, handle, null)
        //vkFreeMemory(backend.logicalDevice.vkDevice, allocatedMemory, null)

        //vmaAllocation.free()
    }
}