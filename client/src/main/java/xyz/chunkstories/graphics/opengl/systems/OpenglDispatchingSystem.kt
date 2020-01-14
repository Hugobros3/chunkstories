package xyz.chunkstories.graphics.opengl.systems

import xyz.chunkstories.api.graphics.representation.Representation
import xyz.chunkstories.api.graphics.systems.dispatching.DispatchingSystem
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.opengl.OpenglGraphicsBackend
import xyz.chunkstories.graphics.opengl.graph.OpenglPass
import xyz.chunkstories.graphics.opengl.graph.OpenglPassInstance

abstract class OpenglDispatchingSystem<R : Representation>(val backend: OpenglGraphicsBackend) : Cleanable {
    abstract val representationName: String

    abstract class Drawer<T>(val pass: OpenglPass) : Cleanable, DispatchingSystem {
        abstract val system: OpenglDispatchingSystem<*>

        override val representationName: String
            get() = system.representationName

        abstract fun executeDrawingCommands(context: OpenglPassInstance, work: Sequence<T>)
    }

    abstract fun createDrawerForPass(pass: OpenglPass, drawerInitCode: Drawer<*>.() -> Unit): Drawer<*>

    val drawersInstances = mutableListOf<Drawer<*>>()

    abstract fun sort(representation: R, drawers: Array<Drawer<*>>, outputs: List<MutableList<Any>>)
}