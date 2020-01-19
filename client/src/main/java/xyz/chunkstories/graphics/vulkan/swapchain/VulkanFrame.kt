package xyz.chunkstories.graphics.vulkan.swapchain

import xyz.chunkstories.api.graphics.rendergraph.Frame
import xyz.chunkstories.api.graphics.shader.ShaderResources
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.resources.frameallocator.FrameDataAllocator
import xyz.chunkstories.graphics.vulkan.util.*

/**
 * The instructions for rendering the next frame: which swapchain image are we rendering in, what semaphore are we waiting on
 * and what semaphore and fence should we signal when we're done
 */
data class VulkanFrame constructor(
        private val backend: VulkanGraphicsBackend,
        override val frameNumber: Int,
        override val animationTimer: Float,
        val swapchainImageIndex: Int,
        val swapchainImage: VkImage,
        val swapchainImageView: VkImageView,
        val swapchainFramebuffer: VkFramebuffer,
        val renderCanBeginSemaphore: VkSemaphore,
        val renderFinishedSemaphore: VkSemaphore,
        val renderFinishedFence: VkFence,
        /** Time when started, in nanoseconds */
        val started: Long) : Frame, Cleanable {

    /** When this frame has completed execution on the GPU, these tasks will be called */
    val recyclingTasks = mutableListOf<() -> Unit>()

    val frameDataAllocator: FrameDataAllocator = backend.frameDataAllocatorProvider.beginFrame(this)
    val stats = Stats()

    override val shaderResources = ShaderResources(null)

    class Stats {
        var totalVerticesDrawn = 0
        var totalDrawcalls = 0
    }

    override fun cleanup() {
        backend.frameDataAllocatorProvider.retireFrame(this)
        recyclingTasks.forEach { it.invoke() }
    }
}