package xyz.chunkstories.graphics.vulkan.systems

import xyz.chunkstories.api.graphics.systems.RegisteredGraphicSystem
import xyz.chunkstories.api.graphics.systems.dispatching.*
import xyz.chunkstories.api.graphics.systems.drawing.DrawingSystem
import xyz.chunkstories.api.graphics.systems.drawing.FarTerrainDrawer
import xyz.chunkstories.api.graphics.systems.drawing.FullscreenQuadDrawer
import xyz.chunkstories.api.gui.GuiDrawer
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.graph.VulkanPass
import xyz.chunkstories.graphics.vulkan.systems.debug.VulkanDebugDrawer
import xyz.chunkstories.graphics.vulkan.systems.debug.VulkanSpinningCubeDrawer
import xyz.chunkstories.graphics.vulkan.systems.gui.VulkanGuiDrawer
import xyz.chunkstories.graphics.vulkan.systems.lighting.VulkanDefferedLightsDispatcher
import xyz.chunkstories.graphics.vulkan.systems.models.VulkanLinesDispatcher
import xyz.chunkstories.graphics.vulkan.systems.models.VulkanModelsDispatcher
import xyz.chunkstories.graphics.vulkan.systems.models.VulkanSpritesDispatcher
import xyz.chunkstories.graphics.vulkan.systems.world.VulkanChunkRepresentationsDispatcher
import xyz.chunkstories.graphics.vulkan.systems.world.farterrain.VulkanFarTerrainRenderer

fun <T : DrawingSystem> VulkanGraphicsBackend.createDrawingSystem(pass: VulkanPass, registration: RegisteredGraphicSystem<T>): VulkanDrawingSystem {
    val dslCode = registration.dslCode as DrawingSystem.() -> Unit

    return when (registration.clazz) {
        GuiDrawer::class.java -> VulkanGuiDrawer(pass, window.client.gui)
        FullscreenQuadDrawer::class.java -> VulkanFullscreenQuadDrawer(pass, dslCode)
        FarTerrainDrawer::class.java -> VulkanFarTerrainRenderer(pass, dslCode)

        Vulkan3DVoxelRaytracer::class.java -> Vulkan3DVoxelRaytracer(pass, dslCode)
        VulkanSpinningCubeDrawer::class.java -> VulkanSpinningCubeDrawer(pass, dslCode)
        VulkanDebugDrawer::class.java -> VulkanDebugDrawer(pass, dslCode, window.client.ingame!!)

        else -> throw Exception("Unimplemented system on this backend: ${registration.clazz}")
    }
}

fun <T: DispatchingSystem> VulkanGraphicsBackend.getOrCreateDispatchingSystem(list: MutableList<VulkanDispatchingSystem<*,*>>, dispatchingSystemRegistration: RegisteredGraphicSystem<T>): VulkanDispatchingSystem<*,*> {
    val implemClass =  when(dispatchingSystemRegistration.clazz) {
        ChunksRenderer::class.java -> VulkanChunkRepresentationsDispatcher::class
        ModelsRenderer::class.java -> VulkanModelsDispatcher::class
        SpritesRenderer::class.java -> VulkanSpritesDispatcher::class
        LinesRenderer::class.java -> VulkanLinesDispatcher::class
        DefferedLightsRenderer::class.java -> VulkanDefferedLightsDispatcher::class
        else -> throw Exception("Unimplemented system on this backend: ${dispatchingSystemRegistration.clazz}")
    }.java

    val existing = list.find { implemClass.isAssignableFrom(it::class.java) }
    if(existing != null)
        return existing

    val new = when(dispatchingSystemRegistration.clazz) {
        ChunksRenderer::class.java -> VulkanChunkRepresentationsDispatcher(this)
        ModelsRenderer::class.java -> VulkanModelsDispatcher(this)
        SpritesRenderer::class.java -> VulkanSpritesDispatcher(this)
        LinesRenderer::class.java -> VulkanLinesDispatcher(this)
        DefferedLightsRenderer::class.java -> VulkanDefferedLightsDispatcher(this)
        else -> throw Exception("Unimplemented system on this backend: ${dispatchingSystemRegistration.clazz}")
    }

    list.add(new)

    return new
}
