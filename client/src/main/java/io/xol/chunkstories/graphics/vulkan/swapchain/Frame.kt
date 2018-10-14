package io.xol.chunkstories.graphics.vulkan.swapchain

import io.xol.chunkstories.graphics.vulkan.util.*

/**
 * The instructions for rendering the next frame: which swapchain image are we rendering in, what semaphore are we waiting on
 * and what semaphore and fence should we signal when we're done
 */
data class Frame(val frameNumber: Int,
                 val swapchainImageIndex: Int,
                 val swapchainImage: VkImage,
                 val swapchainImageView: VkImageView,
                 val swapchainFramebuffer: VkFramebuffer,
                 val inflightFrameIndex: Int,
                 val renderCanBeginSemaphore: VkSemaphore,
                 val renderFinishedSemaphore: VkSemaphore,
                 val renderFinishedFence: VkFence,
                 val started: Long) {
}