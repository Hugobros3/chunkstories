package io.xol.chunkstories.graphics.vulkan.resources

import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.devices.LogicalDevice
import io.xol.chunkstories.graphics.vulkan.util.VkDeviceMemory
import io.xol.chunkstories.graphics.vulkan.util.ensureIs
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkMemoryAllocateInfo
import org.lwjgl.vulkan.VkMemoryRequirements
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties
import java.nio.LongBuffer

class VulkanMemoryManager(val backend: VulkanGraphicsBackend, val device: LogicalDevice) : Cleanable {

    val vkPhysicalDeviceMemoryProperties: VkPhysicalDeviceMemoryProperties = VkPhysicalDeviceMemoryProperties.malloc()

    init {
        vkGetPhysicalDeviceMemoryProperties(backend.physicalDevice.vkPhysicalDevice, vkPhysicalDeviceMemoryProperties)
    }

    fun findMemoryTypeToUse(memoryRequirements: VkMemoryRequirements, requiredFlags: Int): Int {
        for (i in 0 until vkPhysicalDeviceMemoryProperties.memoryTypeCount()) {
            // each bit in memoryTypeBits refers to an acceptable memory type, via it's index in the memoryTypes list of deviceMemoryProperties
            // it's rather confusing at first. We just have to shift the index and AND it with the requirements bits to know if the type is suitable
            if (memoryRequirements.memoryTypeBits() and (1 shl i) != 0) {
                // we check that memory type has all the flags we need too
                if (vkPhysicalDeviceMemoryProperties.memoryTypes(i).propertyFlags() and requiredFlags == requiredFlags) {
                    return i
                }
            }
        }

        return -1
    }

    fun allocateMemoryGivenRequirements(requirements: VkMemoryRequirements, memoryFlags: Int, deviceMemory: LongBuffer) {
        val memoryType = findMemoryTypeToUse(requirements, memoryFlags)
        if (memoryType == -1)
            throw Exception("Unsatisfiable condition: Can't find an appropriate memory type suiting both buffer requirements and usage requirements")

        stackPush()
        val memoryAllocationInfo = VkMemoryAllocateInfo.callocStack().sType(VK10.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO).apply {
            allocationSize(requirements.size())
            memoryTypeIndex(memoryType)
        }

        vkAllocateMemory(backend.logicalDevice.vkDevice, memoryAllocationInfo, null, deviceMemory)
                .ensureIs("Failed to allocate memory !", VK_SUCCESS)

        stackPop()
    }

    fun allocateMemoryGivenRequirements(requirements: VkMemoryRequirements, memoryFlags: Int): VkDeviceMemory {
        stackPush()
        val pDeviceMemory = stackMallocLong(1)
        allocateMemoryGivenRequirements(requirements, memoryFlags, pDeviceMemory)
        val deviceMemory = pDeviceMemory.get(0)
        stackPop()

        return deviceMemory
    }

    override fun cleanup() {
        vkPhysicalDeviceMemoryProperties.free()
    }
}