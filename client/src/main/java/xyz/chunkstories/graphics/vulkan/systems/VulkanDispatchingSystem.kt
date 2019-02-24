package xyz.chunkstories.graphics.vulkan.systems

import org.lwjgl.vulkan.VkCommandBuffer
import xyz.chunkstories.api.graphics.representation.Representation
import xyz.chunkstories.api.graphics.systems.dispatching.DispatchingSystem
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.graph.VulkanFrameGraph
import xyz.chunkstories.graphics.vulkan.graph.VulkanPass
import xyz.chunkstories.graphics.vulkan.resources.Cleanable
import xyz.chunkstories.graphics.vulkan.swapchain.Frame

abstract class VulkanDispatchingSystem<T: Representation>(val backend: VulkanGraphicsBackend) : DispatchingSystem<T>, Cleanable {

    abstract class Drawer(val pass: VulkanPass) : Cleanable {
        abstract fun registerDrawingCommands(frame : Frame, commandBuffer: VkCommandBuffer, passContext: VulkanFrameGraph.FrameGraphNode.PassNode)

        open fun registerAdditionalRenderTasks(passContext: VulkanFrameGraph.FrameGraphNode.PassNode) {
            // Does nothing by default
        }
    }

    abstract fun createDrawerForPass(pass: VulkanPass) : Drawer
}