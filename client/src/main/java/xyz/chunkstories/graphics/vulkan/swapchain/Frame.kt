package xyz.chunkstories.graphics.vulkan.swapchain

import xyz.chunkstories.graphics.vulkan.util.*

/**
 * The instructions for rendering the next frame: which swapchain image are we rendering in, what semaphore are we waiting on
 * and what semaphore and fence should we signal when we're done
 */
data class Frame constructor(val frameNumber: Int,
                 val swapchainImageIndex: Int,
                 val swapchainImage: VkImage,
                 val swapchainImageView: VkImageView,
                 val swapchainFramebuffer: VkFramebuffer,
                 val inflightFrameIndex: Int,
                 val renderCanBeginSemaphore: VkSemaphore,
                 val renderFinishedSemaphore: VkSemaphore,
                 val renderFinishedFence: VkFence,
                 /** Time when started, in nanoseconds */
                 val started: Long) {

    /** When this frame has completed execution on the GPU, these tasks will be called */
    val recyclingTasks = mutableListOf<() -> Unit>()

    val stats = Stats()

    class Stats {
        var totalVerticesDrawn = 0
        var totalDrawcalls = 0
    }
}