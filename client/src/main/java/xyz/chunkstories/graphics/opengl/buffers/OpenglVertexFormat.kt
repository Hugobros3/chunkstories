package xyz.chunkstories.graphics.opengl.buffers

import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL12
import org.lwjgl.opengl.GL12.GL_UNSIGNED_INT_10_10_10_2
import xyz.chunkstories.api.graphics.VertexFormat

import org.lwjgl.opengl.GL30.*

data class OpenglVertexFormat(val type: Int, val isInteger: Boolean, val normalized: Boolean)

val VertexFormat.glVertexFormat: OpenglVertexFormat
    get() = when(this) {
        VertexFormat.FLOAT -> OpenglVertexFormat(GL_FLOAT, false, false)
        VertexFormat.HALF_FLOAT -> OpenglVertexFormat(GL_HALF_FLOAT, false, false)

        VertexFormat.INT -> OpenglVertexFormat(GL_INT, true, false)
        VertexFormat.UINT -> OpenglVertexFormat(GL_UNSIGNED_INT, true, false)

        VertexFormat.SHORT -> OpenglVertexFormat(GL_SHORT, true, false)
        VertexFormat.USHORT -> OpenglVertexFormat(GL_UNSIGNED_SHORT, true, false)
        VertexFormat.NORMALIZED_SHORT -> OpenglVertexFormat(GL_SHORT, false, true)
        VertexFormat.NORMALIZED_USHORT -> OpenglVertexFormat(GL_UNSIGNED_SHORT, false, true)

        VertexFormat.BYTE -> OpenglVertexFormat(GL_BYTE, true, false)
        VertexFormat.UBYTE -> OpenglVertexFormat(GL_UNSIGNED_BYTE, true, false)
        VertexFormat.NORMALIZED_BYTE -> OpenglVertexFormat(GL_BYTE, false, true)
        VertexFormat.NORMALIZED_UBYTE -> OpenglVertexFormat(GL_UNSIGNED_BYTE, false, true)

        VertexFormat.U1010102 -> OpenglVertexFormat(GL_UNSIGNED_INT_10_10_10_2, false, false)
    }

/*fun VertexFormat.glMapping(): Int =
        when (this) {
            VertexFormat.FLOAT -> GL_FLOAT
            VertexFormat.HALF_FLOAT -> GL_HALF_FLOAT

            VertexFormat.INTEGER -> GL_INT

            VertexFormat.SHORT -> GL_SHORT
            VertexFormat.USHORT -> GL_UNSIGNED_SHORT
            VertexFormat.NORMALIZED_USHORT -> GL_UNSIGNED_SHORT

            VertexFormat.BYTE -> GL_BYTE
            VertexFormat.UBYTE -> GL_UNSIGNED_BYTE
            VertexFormat.NORMALIZED_UBYTE -> GL_UNSIGNED_BYTE

            VertexFormat.U1010102 -> GL_UNSIGNED_INT_2_10_10_10_REV

            else -> throw Exception("Unmapped vertex format $this")
        }*/