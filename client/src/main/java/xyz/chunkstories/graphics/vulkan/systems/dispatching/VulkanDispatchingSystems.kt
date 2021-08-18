package xyz.chunkstories.graphics.vulkan.systems.dispatching

import org.lwjgl.vulkan.VkCommandBuffer
import xyz.chunkstories.api.graphics.representation.Representation
import xyz.chunkstories.api.graphics.systems.RegisteredGraphicSystem
import xyz.chunkstories.api.graphics.systems.dispatching.*
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.common.representations.RepresentationsGathered
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.graph.VulkanPass
import xyz.chunkstories.graphics.vulkan.graph.VulkanPassInstance
import xyz.chunkstories.graphics.vulkan.graph.VulkanRenderTaskInstance
import xyz.chunkstories.graphics.vulkan.swapchain.VulkanFrame
import xyz.chunkstories.graphics.vulkan.systems.dispatching.chunks.VulkanChunkRepresentationsDispatcher
import xyz.chunkstories.graphics.vulkan.systems.dispatching.lights.VulkanDefferedLightsDispatcher
import xyz.chunkstories.graphics.vulkan.systems.dispatching.lines.VulkanLinesDispatcher
import xyz.chunkstories.graphics.vulkan.systems.dispatching.models.VulkanModelsDispatcher
import xyz.chunkstories.graphics.vulkan.systems.dispatching.sprites.VulkanSpritesDispatcher

typealias VulkanDispatchingSystemType = VulkanDispatchingSystem<*>

abstract class VulkanDispatchingSystem<R : Representation>(val backend: VulkanGraphicsBackend) : Cleanable {
    val drawersInstances = mutableListOf<Drawer>()

    abstract val representationName: String

    /** The drawer's job is to draw "things". The term is deliberatly loose: the drawer might directly work with the representations
     * the parent system is fed, or it might operate on a different type, perhaps a component of the bigger Representation like it's done
     * for ModelInstances or ChunkRepresentations*/
    abstract class Drawer(val pass: VulkanPass) : Cleanable, DispatchingSystem {
        abstract val system: VulkanDispatchingSystemType

        override val representationName: String
            get() = system.representationName

        abstract fun registerDrawingCommands(drawerWork: DrawerWork)
    }

    abstract class DrawerWork(val drawerInstance: Pair<VulkanPassInstance, Drawer>) {
        abstract fun isEmpty(): Boolean

        lateinit var cmdBuffer: VkCommandBuffer
    }

    abstract fun createDrawerForPass(pass: VulkanPass, drawerInitCode: Drawer.() -> Unit): Drawer

    abstract fun sortWork(frame: VulkanFrame, drawers: Map<VulkanRenderTaskInstance, List<Pair<VulkanPassInstance, Drawer>>>, maskedBuckets: Map<Int, RepresentationsGathered.Bucket>): Map<Pair<VulkanPassInstance, Drawer>, DrawerWork>
}

fun <T: DispatchingSystem> VulkanGraphicsBackend.getOrCreateDispatchingSystem(list: MutableList<VulkanDispatchingSystem<*>>, dispatchingSystemRegistration: RegisteredGraphicSystem<T>): VulkanDispatchingSystem<*>? {
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

    val new: VulkanDispatchingSystem<*> = when(dispatchingSystemRegistration.clazz) {
        ChunksRenderer::class.java -> VulkanChunkRepresentationsDispatcher(this)
        ModelsRenderer::class.java -> VulkanModelsDispatcher(this)
        SpritesRenderer::class.java -> VulkanSpritesDispatcher(this)
        LinesRenderer::class.java -> VulkanLinesDispatcher(this)
        DefferedLightsRenderer::class.java -> VulkanDefferedLightsDispatcher(this)
        else -> {
            //throw Exception("Unimplemented system on this backend: ${dispatchingSystemRegistration.clazz}")
            logger.error("Unimplemented system on this backend: ${dispatchingSystemRegistration.clazz}")
            return null
        }
    }

    list.add(new)

    return new
}