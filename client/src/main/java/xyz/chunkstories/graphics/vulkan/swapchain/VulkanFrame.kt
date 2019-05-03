package xyz.chunkstories.graphics.vulkan.swapchain

import xyz.chunkstories.api.graphics.rendergraph.Frame
import xyz.chunkstories.api.graphics.shader.ShaderResources
import xyz.chunkstories.graphics.vulkan.util.*

/**
 * The instructions for rendering the next frame: which swapchain image are we rendering in, what semaphore are we waiting on
 * and what semaphore and fence should we signal when we're done
 */
data class VulkanFrame constructor(override val frameNumber: Int,
                                   val swapchainImageIndex: Int,
                                   val swapchainImage: VkImage,
                                   val swapchainImageView: VkImageView,
                                   val swapchainFramebuffer: VkFramebuffer,
                                   val renderCanBeginSemaphore: VkSemaphore,
                                   val renderFinishedSemaphore: VkSemaphore,
                                   val renderFinishedFence: VkFence,
                                   /** Time when started, in nanoseconds */
                 val started: Long) : Frame {

    /** When this frame has completed execution on the GPU, these tasks will be called */
    val recyclingTasks = mutableListOf<() -> Unit>()

    val stats = Stats()

    override val shaderResources = ShaderResources(null)

    class Stats {
        var totalVerticesDrawn = 0
        var totalDrawcalls = 0
    }
}