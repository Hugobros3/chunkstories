package io.xol.chunkstories.graphics.vulkan.resources

import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.util.vma.Vma.vmaCreateAllocator
import org.lwjgl.util.vma.Vma.vmaDestroyAllocator
import org.lwjgl.util.vma.VmaAllocatorCreateInfo
import org.lwjgl.util.vma.VmaVulkanFunctions

class VmaAllocator(backend: VulkanGraphicsBackend) : Cleanable {
    val handle: Long

    init {
        stackPush()

        val functions = VmaVulkanFunctions.callocStack().apply {
            set(backend.instance, backend.logicalDevice.vkDevice)
        }

        val vmaCreateInfo = VmaAllocatorCreateInfo.callocStack().apply {
            physicalDevice(backend.physicalDevice.vkPhysicalDevice)
            device(backend.logicalDevice.vkDevice)
            pVulkanFunctions(functions)
        }

        val pAllocator = stackMallocPointer(1)
        vmaCreateAllocator(vmaCreateInfo, pAllocator)

        handle = pAllocator.get(0)

        stackPop()
    }

    override fun cleanup() {
        vmaDestroyAllocator(handle)
    }

}