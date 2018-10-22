package io.xol.chunkstories.graphics.vulkan.textures

import io.xol.chunkstories.api.graphics.TextureFormat
import io.xol.chunkstories.api.graphics.rendergraph.RenderBuffer
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.VulkanRenderGraph
import io.xol.chunkstories.graphics.vulkan.resources.Cleanable
import org.lwjgl.vulkan.VK10.*

class VulkanRenderBuffer(backend: VulkanGraphicsBackend, graph: VulkanRenderGraph, config: RenderBuffer.() -> Unit) : RenderBuffer(), Cleanable {

    override val texture: VulkanTexture2D

    init {
        this.apply(config)

        val usage = when(format) {
            TextureFormat.DEPTH_24, TextureFormat.DEPTH_32 -> VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT
            else -> VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT
        } or VK_IMAGE_USAGE_SAMPLED_BIT or VK_IMAGE_USAGE_TRANSFER_SRC_BIT

        texture = VulkanTexture2D(backend, graph.commandPool, format, size.x, size.y, usage)
        texture.transitionLayout(VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_GENERAL)
    }

    //TODO handle resizes ?

    override fun cleanup() {
        texture.cleanup()
    }
}