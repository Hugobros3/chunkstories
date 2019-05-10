package xyz.chunkstories.graphics.opengl.graph

import xyz.chunkstories.api.graphics.rendergraph.PassDeclaration
import xyz.chunkstories.api.graphics.systems.RegisteredGraphicSystem
import xyz.chunkstories.api.graphics.systems.dispatching.DispatchingSystem
import xyz.chunkstories.api.graphics.systems.drawing.DrawingSystem
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.opengl.OpenglFrame
import xyz.chunkstories.graphics.opengl.OpenglGraphicsBackend
import xyz.chunkstories.graphics.opengl.systems.OpenglDispatchingSystem
import xyz.chunkstories.graphics.opengl.systems.OpenglDrawingSystem

class OpenglPass(val backend: OpenglGraphicsBackend, val renderTask: OpenglRenderTask, val declaration: PassDeclaration) : Cleanable {
    val drawingSystems: List<OpenglDrawingSystem>
    val dispatchingDrawers: List<OpenglDispatchingSystem.Drawer<*>>

    init {
        drawingSystems = mutableListOf()
        dispatchingDrawers = mutableListOf()

        declaration.draws?.registeredSystems?.let {
            for(registeredSystem in it) {
                if (DrawingSystem::class.java.isAssignableFrom(registeredSystem.clazz)) {
                    val drawingSystem = backend.createDrawingSystem(this, registeredSystem as RegisteredGraphicSystem<DrawingSystem>)
                    drawingSystems.add(drawingSystem)
                } else if(DispatchingSystem::class.java.isAssignableFrom(registeredSystem.clazz)) {
                    val dispatchingSystem = backend.getOrCreateDispatchingSystem(renderTask.renderGraph.dispatchingSystems, registeredSystem as RegisteredGraphicSystem<DispatchingSystem>)
                    val drawer = dispatchingSystem.createDrawerForPass(this, registeredSystem.dslCode as OpenglDispatchingSystem.Drawer<*>.() -> Unit)

                    dispatchingSystem.drawersInstances.add(drawer)
                    dispatchingDrawers.add(drawer)
                } else {
                    throw Exception("What is this :$registeredSystem ?")
                }
            }
        }
    }

    fun render(frame: OpenglFrame,
               passInstance: OpenglFrameGraph.FrameGraphNode.OpenglPassInstance,
               representationsGathered: MutableMap<OpenglDispatchingSystem.Drawer<*>, ArrayList<*>>) {

        declaration.setupLambdas.forEach { it.invoke(passInstance) }

        //TODO prepare fbos, bind and execute systems
    }

    override fun cleanup() {
        drawingSystems.forEach(Cleanable::cleanup)
        dispatchingDrawers.forEach(Cleanable::cleanup)
    }
}