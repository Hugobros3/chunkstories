package xyz.chunkstories.graphics.vulkan.graph

import xyz.chunkstories.api.graphics.rendergraph.Pass
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.swapchain.Frame
import xyz.chunkstories.graphics.vulkan.util.VkRenderPass
import xyz.chunkstories.graphics.vulkan.util.VkSemaphore

class PresentationPass(backend: VulkanGraphicsBackend, graph: VulkanRenderGraph, config: Pass.() -> Unit) : VulkanPass(backend, graph, config) {

    override fun createRenderPass(): VkRenderPass = backend.renderToBackbuffer

    override fun createFramebuffer() = -1L

    override fun render(frame: Frame, passBeginSemaphore: VkSemaphore?) {
        framebuffer = backend.swapchain.swapChainFramebuffers[frame.inflightFrameIndex]
        super.render(frame, passBeginSemaphore)
    }
}