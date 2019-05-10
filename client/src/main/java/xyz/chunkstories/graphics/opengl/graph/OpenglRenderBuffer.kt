package xyz.chunkstories.graphics.opengl.graph

import org.joml.Vector2i
import xyz.chunkstories.api.graphics.rendergraph.RenderBufferDeclaration
import xyz.chunkstories.api.graphics.rendergraph.RenderBufferSize
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.opengl.OpenglGraphicsBackend
import xyz.chunkstories.graphics.opengl.textures.OpenglTexture2D
import kotlin.math.roundToInt

class OpenglRenderBuffer(val backend: OpenglGraphicsBackend, val declaration: RenderBufferDeclaration) : Cleanable {
    var texture: OpenglTexture2D

    init {
        val actualSize = declaration.size.actual
        texture = OpenglTexture2D(backend, declaration.format, actualSize.x, actualSize.y)
    }

    val RenderBufferSize.actual: Vector2i
        get() = when(this) {
            is RenderBufferSize.FixedSize -> Vector2i(width, height)
            is RenderBufferSize.ViewportRelativeSize -> Vector2i((backend.window.width * scaleHorizontal).roundToInt(), (backend.window.height * scaleVertical).roundToInt())
        }

    override fun cleanup() {
        texture.cleanup()
    }

    fun resize() {
        texture.cleanup()

        val actualSize = declaration.size.actual
        texture = OpenglTexture2D(backend, declaration.format, actualSize.x, actualSize.y)
    }
}