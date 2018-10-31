package io.xol.chunkstories.graphics.vulkan.systems

import io.xol.chunkstories.api.graphics.systems.drawing.DrawingSystem
import io.xol.chunkstories.graphics.vulkan.graph.VulkanPass
import io.xol.chunkstories.graphics.vulkan.resources.Cleanable
import io.xol.chunkstories.graphics.vulkan.swapchain.Frame
import org.lwjgl.vulkan.VkCommandBuffer

/** Drawing systems are instanced per-declared pass for now */
abstract class VulkanDrawingSystem(val pass: VulkanPass) : DrawingSystem, Cleanable{

    /** Registers drawing commands (pipeline bind, vertex buffer binds, draw calls etc */
    abstract fun registerDrawingCommands(frame : Frame, commandBuffer: VkCommandBuffer)
}