package io.xol.chunkstories.graphics.vulkan.buffers

import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import org.lwjgl.vulkan.VK10

class VulkanVertexBuffer(backend: VulkanGraphicsBackend, bufferSize: Long) : VulkanBuffer(backend, bufferSize, VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, false)