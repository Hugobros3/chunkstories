package xyz.chunkstories.graphics.opengl.buffers

import org.lwjgl.opengl.ARBDirectStateAccess.*
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL31.*

import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.common.shaders.GLSLType
import xyz.chunkstories.graphics.opengl.OpenglGraphicsBackend
import java.nio.ByteBuffer

class OpenglUniformBuffer(val backend: OpenglGraphicsBackend, val mapper: GLSLType.JvmStruct) : Cleanable {
    internal val glId: Int

    init {
        if(backend.openglSupport.dsaSupport)
            glId = glCreateBuffers()
        else
            glId = glGenBuffers()
    }

    fun upload(data: ByteBuffer) {
        if(backend.openglSupport.dsaSupport) {
            glNamedBufferData(glId, data, GL_STREAM_DRAW)
        } else {
            val saved = glGetInteger(GL_UNIFORM_BUFFER_BINDING)
            GL15.glBindBuffer(GL_UNIFORM_BUFFER, glId)
            GL15.glBufferData(GL_UNIFORM_BUFFER, data, GL15.GL_STREAM_DRAW)
            GL15.glBindBuffer(GL_UNIFORM_BUFFER, saved)
        }
    }

    override fun cleanup() {
        glDeleteBuffers(glId)
    }
}