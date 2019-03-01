package xyz.chunkstories.graphics.vulkan.resources

import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.util.vma.Vma.vmaCreateAllocator
import org.lwjgl.util.vma.Vma.vmaDestroyAllocator
import org.lwjgl.util.vma.VmaAllocatorCreateInfo
import org.lwjgl.util.vma.VmaVulkanFunctions
import xyz.chunkstories.graphics.common.Cleanable
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock

class VmaAllocator(backend: VulkanGraphicsBackend) : Cleanable {
    val handle: Long
    val lock = ReentrantLock()

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

    companion object {
        val allocatedBytes = AtomicLong(0)
        val allocations = AtomicInteger(0)
    }

}