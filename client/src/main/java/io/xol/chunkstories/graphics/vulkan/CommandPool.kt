package io.xol.chunkstories.graphics.vulkan

import org.slf4j.LoggerFactory

import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

class CommandPool(val backend: VulkanGraphicsBackend, queueFamily: PhysicalDevice.QueueFamily) {
    val handle: VkCommandPool

    init {
        stackPush()

        val commandPoolCreateInfo = VkCommandPoolCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO).apply {
            queueFamilyIndex(queueFamily.index)
            flags(0)
        }

        val pCommandPool = stackMallocLong(1)
        vkCreateCommandPool(backend.logicalDevice.vkDevice, commandPoolCreateInfo, null, pCommandPool)
                .ensureIs("Failed to create command pool for queue family $queueFamily ", VK_SUCCESS)

        handle = pCommandPool.get(0)

        stackPop()
    }

    fun cleanup() {
        vkDestroyCommandPool(backend.logicalDevice.vkDevice, handle, null)
    }

    companion object {
        val logger = LoggerFactory.getLogger("client.vulkan")
    }
}