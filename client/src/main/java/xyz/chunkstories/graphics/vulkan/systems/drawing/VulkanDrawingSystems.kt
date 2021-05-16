package xyz.chunkstories.graphics.vulkan.systems.drawing

import xyz.chunkstories.api.graphics.systems.drawing.DrawingSystem
import xyz.chunkstories.graphics.vulkan.graph.VulkanPass
import xyz.chunkstories.graphics.common.Cleanable
import org.lwjgl.vulkan.VkCommandBuffer
import xyz.chunkstories.api.graphics.systems.RegisteredGraphicSystem
import xyz.chunkstories.api.graphics.systems.drawing.FarTerrainDrawer
import xyz.chunkstories.api.graphics.systems.drawing.FullscreenQuadDrawer
import xyz.chunkstories.api.gui.GuiDrawer
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.graph.VulkanPassInstance
import xyz.chunkstories.graphics.vulkan.systems.drawing.dbgwireframe.VulkanDebugDrawer
import xyz.chunkstories.graphics.vulkan.systems.drawing.debugcube.VulkanSpinningCubeDrawer
import xyz.chunkstories.graphics.vulkan.systems.drawing.farterrain.VulkanFarTerrainRenderer
import xyz.chunkstories.graphics.vulkan.systems.drawing.fsquad.VulkanFullscreenQuadDrawer
import xyz.chunkstories.graphics.vulkan.systems.drawing.gui.VulkanGuiDrawer

/** Drawing systems are instanced per-declared pass for now */
abstract class VulkanDrawingSystem(val pass: VulkanPass) : DrawingSystem, Cleanable {

    /** Registers drawing commands (pipeline bind, vertex buffer binds, draw calls etc */
    abstract fun registerDrawingCommands(context: VulkanPassInstance, commandBuffer: VkCommandBuffer)
}

fun <T : DrawingSystem> VulkanGraphicsBackend.createDrawingSystem(pass: VulkanPass, registration: RegisteredGraphicSystem<T>): VulkanDrawingSystem? {
    val dslCode = registration.dslCode as DrawingSystem.() -> Unit

    return when (registration.clazz) {
        GuiDrawer::class.java -> VulkanGuiDrawer(pass, window.client.gui)
        FullscreenQuadDrawer::class.java -> VulkanFullscreenQuadDrawer(pass, dslCode)
        FarTerrainDrawer::class.java -> VulkanFarTerrainRenderer(pass, dslCode)

        VulkanSpinningCubeDrawer::class.java -> VulkanSpinningCubeDrawer(pass, dslCode)
        VulkanDebugDrawer::class.java -> VulkanDebugDrawer(pass, dslCode, window.client.ingame!!)

        else -> {
            //throw Exception("Unimplemented system on this backend: ${registration.clazz}")
            logger.error("Unimplemented system on this backend: ${registration.clazz}")
            null
        }
    }
}