package xyz.chunkstories.graphics.vulkan

import xyz.chunkstories.graphics.vulkan.devices.LogicalDevice
import xyz.chunkstories.graphics.vulkan.devices.PhysicalDevice
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.vulkan.util.VkCommandPool
import xyz.chunkstories.graphics.vulkan.util.VkFence
import xyz.chunkstories.graphics.vulkan.util.ensureIs
import org.slf4j.LoggerFactory

import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class CommandPool(val backend: VulkanGraphicsBackend, queueFamily: PhysicalDevice.QueueFamily, flags: Int) : Cleanable {
    val handle: VkCommandPool

    val lock = ReentrantLock()
    val freeCmdBuffers = mutableListOf<VkCommandBuffer>()
    var nextAllocateAmount = 1

    val secondaryLock = ReentrantLock()
    val secondaryFreeCmdBuffers = mutableListOf<VkCommandBuffer>()
    var nextSecondaryAllocateAmount = 1

    init {
        stackPush()

        val commandPoolCreateInfo = VkCommandPoolCreateInfo.callocStack().apply {
            sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
            queueFamilyIndex(queueFamily.index)
            flags(flags)
        }

        val pCommandPool = stackMallocLong(1)
        vkCreateCommandPool(backend.logicalDevice.vkDevice, commandPoolCreateInfo, null, pCommandPool).ensureIs("Failed to create command pool for queue family $queueFamily ", VK_SUCCESS)
        handle = pCommandPool.get(0)

        stackPop()
    }

    private fun makeMorePrimaryCommandBuffers() {
        for(i in 0 until nextAllocateAmount) {
            stackPush().use {
                val allocInfo = VkCommandBufferAllocateInfo.callocStack().sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO).apply {
                    level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    commandPool(handle)
                    commandBufferCount(1)
                }

                val pCommandBuffer = stackMallocPointer(1)
                vkAllocateCommandBuffers(backend.logicalDevice.vkDevice, allocInfo, pCommandBuffer).ensureIs("Failed to allocate CB !", VK_SUCCESS)
                val commandBuffer = VkCommandBuffer(pCommandBuffer.get(0), backend.logicalDevice.vkDevice)

                freeCmdBuffers.add(commandBuffer)
            }
        }
        nextAllocateAmount *= 2
    }

    private fun makeMoreSecondaryCommandBuffers() {
        for(i in 0 until nextSecondaryAllocateAmount) {
            stackPush().use {
                val allocInfo = VkCommandBufferAllocateInfo.callocStack().sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO).apply {
                    level(VK_COMMAND_BUFFER_LEVEL_SECONDARY)
                    commandPool(handle)
                    commandBufferCount(1)
                }

                val pCommandBuffer = stackMallocPointer(1)
                vkAllocateCommandBuffers(backend.logicalDevice.vkDevice, allocInfo, pCommandBuffer).ensureIs("Failed to allocate CB !", VK_SUCCESS)
                val commandBuffer = VkCommandBuffer(pCommandBuffer.get(0), backend.logicalDevice.vkDevice)

                secondaryFreeCmdBuffers.add(commandBuffer)
            }
        }
        nextSecondaryAllocateAmount *= 2
    }

    fun loanPrimaryCommandBuffer() : VkCommandBuffer {
        lock.withLock {
            if(freeCmdBuffers.isEmpty())
                makeMorePrimaryCommandBuffers()

            return freeCmdBuffers.removeAt(0)
        }
    }

    fun returnPrimaryCommandBuffer(cmdBuffer: VkCommandBuffer) {
        vkResetCommandBuffer(cmdBuffer, 0)

        lock.withLock {
            freeCmdBuffers.add(cmdBuffer)
        }
    }

    fun loanSecondaryCommandBuffer() : VkCommandBuffer {
        secondaryLock.withLock {
            if(secondaryFreeCmdBuffers.isEmpty())
                makeMoreSecondaryCommandBuffers()

            return secondaryFreeCmdBuffers.removeAt(0)
        }
    }

    fun returnSecondaryCommandBuffer(cmdBuffer: VkCommandBuffer) {
        vkResetCommandBuffer(cmdBuffer, 0)

        secondaryLock.withLock {
            secondaryFreeCmdBuffers.add(cmdBuffer)
        }
    }

    fun startPrimaryCommandBuffer() : VkCommandBuffer {
        stackPush()
        val commandBuffer = loanPrimaryCommandBuffer()

        val beginInfo = VkCommandBufferBeginInfo.callocStack().sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO).apply {
            flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)
        }

        vkBeginCommandBuffer(commandBuffer, beginInfo)

        stackPop()
        return commandBuffer
    }

    fun submitAndReturnPrimaryCommandBuffer(commandBuffer: VkCommandBuffer, queue: LogicalDevice.Queue, fence : VkFence) {
        stackPush()

        val submitInfo = VkSubmitInfo.callocStack(1).sType(VK_STRUCTURE_TYPE_SUBMIT_INFO).apply {
            pCommandBuffers(stackPointers(commandBuffer))
        }

        queue.mutex.acquireUninterruptibly()
        vkQueueSubmit(queue.handle, submitInfo, fence).ensureIs("Failed to submit commandBuffer ", VK_SUCCESS)
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