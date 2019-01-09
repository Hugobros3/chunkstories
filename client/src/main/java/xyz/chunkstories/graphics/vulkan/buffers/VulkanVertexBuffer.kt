package xyz.chunkstories.graphics.vulkan.buffers

import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import org.lwjgl.vulkan.VK10

class VulkanVertexBuffer(backend: VulkanGraphicsBackend, bufferSize: Long, hostVisible: Boolean = false) : VulkanBuffer(backend, bufferSize, VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, hostVisible) {

}