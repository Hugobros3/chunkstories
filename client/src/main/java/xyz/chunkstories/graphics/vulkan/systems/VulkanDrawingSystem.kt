package xyz.chunkstories.graphics.vulkan.systems

import xyz.chunkstories.api.graphics.systems.drawing.DrawingSystem
import xyz.chunkstories.graphics.vulkan.graph.VulkanPass
import xyz.chunkstories.graphics.vulkan.resources.Cleanable
import xyz.chunkstories.graphics.vulkan.swapchain.Frame
import org.lwjgl.vulkan.VkCommandBuffer
import xyz.chunkstories.api.graphics.rendergraph.RenderingContext
import xyz.chunkstories.graphics.vulkan.graph.FrameGraph
import xyz.chunkstories.graphics.vulkan.graph.VulkanRenderBuffer
import xyz.chunkstories.graphics.vulkan.graph.VulkanRenderGraph

/** Drawing systems are instanced per-declared pass for now */
abstract class VulkanDrawingSystem(val pass: VulkanPass) : DrawingSystem, Cleanable{

    /** Registers drawing commands (pipeline bind, vertex buffer binds, draw calls etc */
    abstract fun registerDrawingCommands(frame : Frame, commandBuffer: VkCommandBuffer, passContext: FrameGraph.FrameGraphNode.PassNode)

    open fun registerAdditionalRenderTasks(passContext: FrameGraph.FrameGraphNode.PassNode) {
        // Does nothing by default
    }

    //open fun provideAdditionalConsumedInputRenderBuffers(renderingContext: RenderingContext) = emptyList<VulkanRenderBuffer>()
}