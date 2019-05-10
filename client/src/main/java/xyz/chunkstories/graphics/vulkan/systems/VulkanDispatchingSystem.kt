package xyz.chunkstories.graphics.vulkan.systems

import org.lwjgl.vulkan.VkCommandBuffer
import xyz.chunkstories.api.graphics.rendergraph.SystemExecutionContext
import xyz.chunkstories.api.graphics.representation.Representation
import xyz.chunkstories.api.graphics.systems.dispatching.DispatchingSystem
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.graph.VulkanPass
import xyz.chunkstories.graphics.vulkan.swapchain.VulkanFrame

abstract class VulkanDispatchingSystem<R : Representation>(val backend: VulkanGraphicsBackend) : Cleanable {

    abstract val representationName: String

    /** The drawer's job is to draw "things". The term is deliberatly loose: the drawer might directly work with the representations
     * the parent system is fed, or it might operate on a different type, perhaps a component of the bigger Representation like it's done
     * for ModelInstances or ChunkRepresentations*/
    abstract class Drawer<T>(val pass: VulkanPass) : Cleanable, DispatchingSystem {
        abstract val system: VulkanDispatchingSystem<*>

        override val representationName: String
            get() = system.representationName

        abstract fun registerDrawingCommands(frame: VulkanFrame, context: SystemExecutionContext, commandBuffer: VkCommandBuffer, work: Sequence<T>)

        val setupLambdas = mutableListOf<SystemExecutionContext.() -> Unit>()
        fun setup(dslCode: SystemExecutionContext.() -> Unit) {
            setupLambdas.add(dslCode)
        }

        fun executePerFrameSetup(ctx: SystemExecutionContext) {
            setupLambdas.forEach { it.invoke(ctx) }
        }
    }

    abstract fun createDrawerForPass(pass: VulkanPass, drawerInitCode: Drawer<*>.() -> Unit): Drawer<*>

    val drawersInstances = mutableListOf<Drawer<*>>()

    abstract fun sort(representation: R, drawers: Array<Drawer<*>>, outputs: List<MutableList<Any>>)
}