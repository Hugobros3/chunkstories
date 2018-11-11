package io.xol.chunkstories.graphics.vulkan.buffers

import io.xol.chunkstories.graphics.vulkan.util.VkBuffer
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.resources.Cleanable
import io.xol.chunkstories.graphics.vulkan.resources.VmaAllocator
import io.xol.chunkstories.graphics.vulkan.util.VkDeviceMemory
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.util.vma.Vma.*
import org.lwjgl.util.vma.VmaAllocationCreateInfo
import org.lwjgl.util.vma.VmaAllocationInfo
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkBufferCreateInfo
import org.lwjgl.vulkan.VkMemoryRequirements
import java.nio.ByteBuffer

open class VulkanBuffer(val backend: VulkanGraphicsBackend, val bufferSize: Long, usageBits: Int) : Cleanable {
    val handle: VkBuffer
    val vmaAllocation: Long
    // private val allocatedMemory: VkDeviceMemory

    constructor(backend: VulkanGraphicsBackend, initialData: ByteBuffer, usageBits: Int) : this(backend, initialData.capacity().toLong(), usageBits) {
        upload(initialData)
    }

    init {
        stackPush()
        val bufferInfo = VkBufferCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO).apply {
            size(bufferSize)
            usage(usageBits)
            sharingMode(VK_SHARING_MODE_EXCLUSIVE)
            flags(0)
        }

        val vmaAllocCreateInfo = VmaAllocationCreateInfo.callocStack().apply {
            usage(VMA_MEMORY_USAGE_GPU_ONLY)
        }

        val pBuffer = stackMallocLong(1)
        //vmaAllocation = VmaAllocationInfo.calloc()
        val pAllocation = stackPointers(1)

        vmaCreateBuffer(backend.vmaAllocator.handle, bufferInfo, vmaAllocCreateInfo, pBuffer, pAllocation, null)
        handle = pBuffer.get(0)
        vmaAllocation = pAllocation.get(0)

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

        val ppData = stackMallocPointer(1)

        vmaMapMemory(backend.vmaAllocator.handle, vmaAllocation, ppData)
        //vkMapMemory(backend.logicalDevice.vkDevice, allocatedMemory, 0, bufferSize, 0, ppData)

        val mappedMemory = ppData.getByteBuffer(bufferSize.toInt())
        mappedMemory.put(data)

        vmaUnmapMemory(backend.vmaAllocator.handle, vmaAllocation)
        //vkUnmapMemory(backend.logicalDevice.vkDevice, allocatedMemory)

        stackPop()
    }

    override fun cleanup() {
        vmaDestroyBuffer(backend.vmaAllocator.handle, handle, vmaAllocation)
        // vkDestroyBuffer(backend.logicalDevice.vkDevice, handle, null)
        //vkFreeMemory(backend.logicalDevice.vkDevice, allocatedMemory, null)

        //vmaAllocation.free()
    }
}