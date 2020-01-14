package xyz.chunkstories.graphics.vulkan.systems

import org.lwjgl.vulkan.VkCommandBuffer
import xyz.chunkstories.api.graphics.rendergraph.SystemExecutionContext
import xyz.chunkstories.api.graphics.representation.Representation
import xyz.chunkstories.api.graphics.systems.dispatching.DispatchingSystem
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.graph.VulkanPass
import xyz.chunkstories.graphics.vulkan.swapchain.VulkanFrame

abstract class VulkanDispatchingSystem<R : Representation, I>(val backend: VulkanGraphicsBackend) : Cleanable {
    abstract val representationName: String

    /** The drawer's job is to draw "things". The term is deliberatly loose: the drawer might directly work with the representations
     * the parent system is fed, or it might operate on a different type, perhaps a component of the bigger Representation like it's done
     * for ModelInstances or ChunkRepresentations*/
    abstract class Drawer<T>(val pass: VulkanPass) : Cleanable, DispatchingSystem {
        abstract val system: VulkanDispatchingSystem<*,*>

        override val representationName: String
            get() = system.representationName

        protected abstract fun registerDrawingCommands(frame: VulkanFrame, context: SystemExecutionContext, commandBuffer: VkCommandBuffer, work: T)

        fun registerDrawingCommands_(frame: VulkanFrame, context: SystemExecutionContext, commandBuffer: VkCommandBuffer, work: Any) {
            registerDrawingCommands(frame, context, commandBuffer, work as T)
        }
    }

    abstract fun createDrawerForPass(pass: VulkanPass, drawerInitCode: Drawer<I>.() -> Unit): Drawer<I>

    //val drawersInstances = mutableListOf<Drawer<I>>()

    // abstract fun sort(representation: R, drawers: Array<Drawer<*>>, outputs: List<MutableList<Any>>)

    /** Takes in representations submitted by the user, a list of drawers belonging to DispatchingSystems that match the representation type,
     * and produces work for those drivers in whatever the actual internal representation they use by putting it on the mutable map */
    protected abstract fun sort(representations: Sequence<R>, drawers: List<Drawer<I>>, workForDrawers: MutableMap<Drawer<I>, I>)

    /** Type erased facade for sort */
    fun sort_(representations: Sequence<R>, drawers: List<Drawer<*>>, workForDrawers: MutableMap<Drawer<*>, *>) {
        sort(representations, drawers as List<Drawer<I>>, workForDrawers as MutableMap<Drawer<I>, I>)
    }

}