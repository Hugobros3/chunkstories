package xyz.chunkstories.graphics.opengl.buffers

import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.ARBDirectStateAccess.*

import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.opengl.OpenglGraphicsBackend
import java.nio.ByteBuffer

class OpenglVertexBuffer(val backend: OpenglGraphicsBackend) : Cleanable{

    internal val glId: Int

    init {
        glId = glCreateBuffers()
    }

    fun upload(data: ByteBuffer) {
        glNamedBufferData(glId, data, GL_STATIC_DRAW)
    }

    override fun cleanup() {
        glDeleteBuffers(glId)
    }
}