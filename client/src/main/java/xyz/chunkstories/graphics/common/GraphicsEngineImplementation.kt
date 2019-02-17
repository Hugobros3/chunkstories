package xyz.chunkstories.graphics.common

import xyz.chunkstories.api.graphics.GraphicsBackend
import xyz.chunkstories.api.graphics.GraphicsEngine
import xyz.chunkstories.api.graphics.rendergraph.RenderGraphDeclaration

class GraphicsEngineImplementation: GraphicsEngine {
    override val backend: GraphicsBackend
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val models: GraphicsEngine.Models
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val textures: GraphicsEngine.Textures
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun loadRenderGraph(declaration: RenderGraphDeclaration) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}