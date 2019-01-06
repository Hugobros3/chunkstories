package io.xol.chunkstories.graphics.vulkan.shaders

import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.util.VkDescriptorSetLayout
import org.lwjgl.vulkan.VK10

data class DescriptorSlotLayout constructor(val vulkanLayout: VkDescriptorSetLayout, val descriptorsCountByType: Map<Int, Int>) {
    val bindingsCountTotal: Int = descriptorsCountByType.values.sum()

    fun VulkanGraphicsBackend.cleanup() {
        VK10.vkDestroyDescriptorSetLayout(logicalDevice.vkDevice, vulkanLayout, null)
    }
}