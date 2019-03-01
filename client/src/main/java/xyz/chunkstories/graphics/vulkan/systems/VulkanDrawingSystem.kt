package xyz.chunkstories.graphics.vulkan.systems

import xyz.chunkstories.api.graphics.systems.drawing.DrawingSystem
import xyz.chunkstories.graphics.vulkan.graph.VulkanPass
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.vulkan.swapchain.Frame
import org.lwjgl.vulkan.VkCommandBuffer
import xyz.chunkstories.graphics.vulkan.graph.VulkanFrameGraph

/** Drawing systems are instanced per-declared pass for now */
abstract class VulkanDrawingSystem(val pass: VulkanPass) : DrawingSystem, Cleanable {

    /** Registers drawing commands (pipeline bind, vertex buffer binds, draw calls etc */
    abstract fun registerDrawingCommands(frame : Frame, commandBuffer: VkCommandBuffer, passContext: VulkanFrameGraph.FrameGraphNode.PassNode)

    open fun registerAdditionalRenderTasks(passContext: VulkanFrameGraph.FrameGraphNode.PassNode) {
        // Does nothing by default
    }
}