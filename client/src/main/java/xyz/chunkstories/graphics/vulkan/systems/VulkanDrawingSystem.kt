package xyz.chunkstories.graphics.vulkan.systems

import xyz.chunkstories.api.graphics.systems.drawing.DrawingSystem
import xyz.chunkstories.graphics.vulkan.graph.VulkanPass
import xyz.chunkstories.graphics.common.Cleanable
import org.lwjgl.vulkan.VkCommandBuffer
import xyz.chunkstories.graphics.vulkan.graph.VulkanPassInstance

/** Drawing systems are instanced per-declared pass for now */
abstract class VulkanDrawingSystem(val pass: VulkanPass) : DrawingSystem, Cleanable {

    /** Registers drawing commands (pipeline bind, vertex buffer binds, draw calls etc */
    abstract fun registerDrawingCommands(context: VulkanPassInstance, commandBuffer: VkCommandBuffer)
}