package io.xol.chunkstories.graphics.vulkan.systems

import io.xol.chunkstories.api.graphics.systems.drawing.DrawingSystem
import io.xol.chunkstories.graphics.vulkan.VulkanPass
import io.xol.chunkstories.graphics.vulkan.resources.Cleanable
import io.xol.chunkstories.graphics.vulkan.swapchain.Frame
import org.lwjgl.vulkan.VkCommandBuffer

/** Drawing systems are instanced per-declared pass for now */
abstract class VulkanDrawingSystem(val pass: VulkanPass) : DrawingSystem, Cleanable{
    abstract fun render(frame : Frame, commandBuffer: VkCommandBuffer)

    override fun cleanup() {

    }
}