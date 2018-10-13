package io.xol.chunkstories.graphics.vulkan.buffers

import io.xol.chunkstories.graphics.vulkan.VkBuffer
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.resources.Cleanable
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkBufferCreateInfo
import org.lwjgl.vulkan.VkMemoryAllocateInfo
import org.lwjgl.vulkan.VkMemoryRequirements
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties
import java.nio.ByteBuffer

open class VulkanBuffer(val backend: VulkanGraphicsBackend, val bufferSize: Long, usageBits: Int) : Cleanable {
    val handle: VkBuffer
    private val allocatedMemoryPointer: Long

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

        //TODO move
        val deviceMemoryProperties = VkPhysicalDeviceMemoryProperties.callocStack()
        vkGetPhysicalDeviceMemoryProperties(backend.physicalDevice.vkPhysicalDevice, deviceMemoryProperties)

        // We want to write to this buffer !
        val requiredFlags = VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT

        var memortType = -1
        for(i in 0 until deviceMemoryProperties.memoryTypeCount()) {
            // each bit in memoryTypeBits refers to an acceptable memory type, via it's index in the memoryTypes list of deviceMemoryProperties
            // it's rather confusing at first. We just have to shift the index and AND it with the requirements bits to know if the type is suitable
            if(memoryRequirements.memoryTypeBits() and (1 shl i) != 0) {
                // we check that memory type has all the flags we need too
                if(deviceMemoryProperties.memoryTypes(i).propertyFlags() and requiredFlags == requiredFlags) {
                    memortType = i
                    break
                }
            }
        }

        if(memortType == -1) throw Exception("Unsatisfiable condition: Can't find an appropriate memory type suiting both buffer requirements and usage requirements")

        val memoryAllocationInfo = VkMemoryAllocateInfo.callocStack().sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO).apply {
            allocationSize(memoryRequirements.size())
            memoryTypeIndex(memortType)
        }

        val allocatedDeviceMemory = stackMallocLong(1)
        vkAllocateMemory(backend.logicalDevice.vkDevice, memoryAllocationInfo, null, allocatedDeviceMemory)
        allocatedMemoryPointer = allocatedDeviceMemory.get(0)

        vkBindBufferMemory(backend.logicalDevice.vkDevice, handle, allocatedMemoryPointer, 0)

        stackPop()
    }

    /** Memory-maps the buffer and updates it */
    fun upload(data: ByteBuffer) {
        assert(data.remaining() <= bufferSize)

        stackPush()

        val ppData = stackMallocPointer(1)
        vkMapMemory(backend.logicalDevice.vkDevice, allocatedMemoryPointer, 0, bufferSize, 0, ppData)

        val mappedMemory = ppData.getByteBuffer(bufferSize.toInt())
        mappedMemory.put(data)

        vkUnmapMemory(backend.logicalDevice.vkDevice, allocatedMemoryPointer)

        stackPop()
    }

    override fun cleanup() {
        vkDestroyBuffer(backend.logicalDevice.vkDevice, handle, null)
        vkFreeMemory(backend.logicalDevice.vkDevice, allocatedMemoryPointer, null)
    }
}