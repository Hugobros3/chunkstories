package io.xol.chunkstories.graphics.vulkan.buffers

import io.xol.chunkstories.graphics.vulkan.util.VkBuffer
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.resources.Cleanable
import io.xol.chunkstories.graphics.vulkan.util.VkDeviceMemory
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkBufferCreateInfo
import org.lwjgl.vulkan.VkMemoryRequirements
import java.nio.ByteBuffer

open class VulkanBuffer(val backend: VulkanGraphicsBackend, val bufferSize: Long, usageBits: Int) : Cleanable {
    val handle: VkBuffer
    private val allocatedMemory: VkDeviceMemory

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

        val pBuffer = stackMallocLong(1)
        vkCreateBuffer(backend.logicalDevice.vkDevice, bufferInfo, null, pBuffer)
        handle = pBuffer.get(0)

        val memoryRequirements = VkMemoryRequirements.callocStack()
        vkGetBufferMemoryRequirements(backend.logicalDevice.vkDevice, handle, memoryRequirements)

        /*val memortType = backend.memoryManager.findMemoryTypeToUse(memoryRequirements, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT)
        if(memortType == -1) throw Exception("Unsatisfiable condition: Can't find an appropriate memory type suiting both buffer requirements and usage requirements")

        val memoryAllocationInfo = VkMemoryAllocateInfo.callocStack().sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO).apply {
            allocationSize(memoryRequirements.size())
            memoryTypeIndex(memortType)
        }

        val allocatedDeviceMemory = stackMallocLong(1)
        vkAllocateMemory(backend.logicalDevice.vkDevice, memoryAllocationInfo, null, allocatedDeviceMemory)*/

        allocatedMemory = backend.memoryManager.allocateMemoryGivenRequirements(memoryRequirements,  VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT)

        vkBindBufferMemory(backend.logicalDevice.vkDevice, handle, allocatedMemory, 0)

        stackPop()
    }

    /** Memory-maps the buffer and updates it */
    fun upload(data: ByteBuffer) {
        assert(data.remaining() <= bufferSize)

        stackPush()

        val ppData = stackMallocPointer(1)
        vkMapMemory(backend.logicalDevice.vkDevice, allocatedMemory, 0, bufferSize, 0, ppData)

        val mappedMemory = ppData.getByteBuffer(bufferSize.toInt())
        mappedMemory.put(data)

        vkUnmapMemory(backend.logicalDevice.vkDevice, allocatedMemory)

        stackPop()
    }

    override fun cleanup() {
        vkDestroyBuffer(backend.logicalDevice.vkDevice, handle, null)
        vkFreeMemory(backend.logicalDevice.vkDevice, allocatedMemory, null)
    }
}