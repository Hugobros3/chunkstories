package xyz.chunkstories.graphics.opengl.buffers

import org.lwjgl.opengl.ARBDirectStateAccess.*
import org.lwjgl.opengl.GL33.*

import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.opengl.OpenglGraphicsBackend
import java.nio.ByteBuffer

class OpenglVertexBuffer(val backend: OpenglGraphicsBackend) : Cleanable{
    internal val glId: Int

    init {
        if(backend.openglSupport.dsaSupport)
            glId = glCreateBuffers()
        else
            glId = glGenBuffers()
    }

    fun upload(data: ByteBuffer) {
        if(backend.openglSupport.dsaSupport) {
            glNamedBufferData(glId, data, GL_STATIC_DRAW)
        } else {
            val saved = glGetInteger(GL_ARRAY_BUFFER_BINDING)
            glBindBuffer(GL_ARRAY_BUFFER, glId)
            glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW)
            glBindBuffer(GL_ARRAY_BUFFER, saved)
        }

    }

    override fun cleanup() {
        glDeleteBuffers(glId)
    }
}