package xyz.chunkstories.graphics.opengl.graph

import xyz.chunkstories.api.graphics.rendergraph.RenderTaskDeclaration
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.opengl.OpenglGraphicsBackend

class OpenglRenderTask(val backend: OpenglGraphicsBackend, val renderGraph: OpenglRenderGraph, val declaration: RenderTaskDeclaration) : Cleanable {
    val buffers: Map<String, OpenglRenderBuffer>
    val passes: Map<String, OpenglPass>

    val rootPass: OpenglPass

    init {
        buffers = declaration.renderBuffersDeclarations.renderBuffers.map {
            val openglRenderbuffer = OpenglRenderBuffer(backend, it)
            Pair(it.name, openglRenderbuffer)
        }.toMap()

        passes = declaration.passesDeclarations.passes.map {
            val openglPass = OpenglPass(backend, this@OpenglRenderTask, it)
            Pair(it.name, openglPass)
        }.toMap()

        rootPass = passes.values.find { it.declaration.name == declaration.finalPassName }!!
    }

    override fun cleanup() {
        buffers.values.forEach(Cleanable::cleanup)
        passes.values.forEach(Cleanable::cleanup)

        for(cleanupHook in declaration.cleanupHooks) {
            cleanupHook()
        }
    }
}