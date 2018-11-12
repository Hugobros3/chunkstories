package io.xol.chunkstories.graphics.vulkan

import io.xol.chunkstories.graphics.vulkan.devices.LogicalDevice
import io.xol.chunkstories.graphics.vulkan.devices.PhysicalDevice
import io.xol.chunkstories.graphics.vulkan.resources.Cleanable
import io.xol.chunkstories.graphics.vulkan.util.VkCommandPool
import io.xol.chunkstories.graphics.vulkan.util.VkFence
import io.xol.chunkstories.graphics.vulkan.util.ensureIs
import org.slf4j.LoggerFactory

import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

class CommandPool(val backend: VulkanGraphicsBackend, queueFamily: PhysicalDevice.QueueFamily, flags: Int) : Cleanable {
    val handle: VkCommandPool

    init {
        stackPush()

        val commandPoolCreateInfo = VkCommandPoolCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO).apply {
            queueFamilyIndex(queueFamily.index)
            flags(flags)
        }

        val pCommandPool = stackMallocLong(1)
        vkCreateCommandPool(backend.logicalDevice.vkDevice, commandPoolCreateInfo, null, pCommandPool)
                .ensureIs("Failed to create command pool for queue family $queueFamily ", VK_SUCCESS)

        handle = pCommandPool.get(0)

        stackPop()
    }

    fun createOneUseCB() : VkCommandBuffer {
        stackPush()

        val allocInfo = VkCommandBufferAllocateInfo.callocStack().sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO).apply {
            level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
            commandPool(handle)
            commandBufferCount(1)
        }

        val pCommandBuffer = stackMallocPointer(1)
        vkAllocateCommandBuffers(backend.logicalDevice.vkDevice, allocInfo, pCommandBuffer).ensureIs("Failed to allocate CB !", VK_SUCCESS)
        val commandBuffer: VkCommandBuffer = VkCommandBuffer(pCommandBuffer.get(0), backend.logicalDevice.vkDevice)

        val beginInfo = VkCommandBufferBeginInfo.callocStack().sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO).apply {
            flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)
        }

        vkBeginCommandBuffer(commandBuffer, beginInfo)

        stackPop()
        return commandBuffer
    }

    fun submitOneTimeCB(commandBuffer: VkCommandBuffer, queue: LogicalDevice.Queue, fence : VkFence) {
        vkEndCommandBuffer(commandBuffer)

        stackPush()

        val submitInfo = VkSubmitInfo.callocStack(1).sType(VK_STRUCTURE_TYPE_SUBMIT_INFO).apply {
            pCommandBuffers(stackPointers(commandBuffer))
        }

        queue.mutex.acquireUninterruptibly()
        vkQueueSubmit(queue.handle, submitInfo, fence ?: VK_NULL_HANDLE).ensureIs("Failed to submit commandBuffer ", VK_SUCCESS)
        queue.mutex.release()

        stackPop()
    }

    override fun cleanup() {
        vkDestroyCommandPool(backend.logicalDevice.vkDevice, handle, null)
    }

    companion object {
        val logger = LoggerFactory.getLogger("client.vulkan")
    }
}