package xyz.chunkstories.graphics.opengl.buffers

import xyz.chunkstories.api.graphics.VertexFormat

import org.lwjgl.opengl.GL30.*

data class OpenglVertexFormat(val type: Int, val normalized: Boolean)

val VertexFormat.glVertexFormat: OpenglVertexFormat
    get() = when(this) {
        VertexFormat.FLOAT -> OpenglVertexFormat(GL_FLOAT, false)
        VertexFormat.HALF_FLOAT -> TODO()
        VertexFormat.INTEGER -> TODO()
        VertexFormat.SHORT -> TODO()
        VertexFormat.USHORT -> TODO()
        VertexFormat.NORMALIZED_USHORT -> TODO()
        VertexFormat.BYTE -> TODO()
        VertexFormat.UBYTE -> TODO()
        VertexFormat.NORMALIZED_UBYTE -> TODO()
        VertexFormat.U1010102 -> TODO()
    }