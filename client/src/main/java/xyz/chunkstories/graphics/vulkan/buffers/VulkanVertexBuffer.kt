package xyz.chunkstories.graphics.vulkan.buffers

import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import org.lwjgl.vulkan.VK10
import xyz.chunkstories.graphics.vulkan.memory.MemoryUsagePattern

class VulkanVertexBuffer(backend: VulkanGraphicsBackend, bufferSize: Long, memoryUsagePattern: MemoryUsagePattern) : VulkanBuffer(backend, bufferSize, VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, memoryUsagePattern) {

}