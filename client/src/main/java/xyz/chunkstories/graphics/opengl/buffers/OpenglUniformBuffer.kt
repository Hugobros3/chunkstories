package xyz.chunkstories.graphics.opengl.buffers

import org.lwjgl.opengl.ARBDirectStateAccess.*
import org.lwjgl.opengl.GL31.*

import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.common.shaders.GLSLType
import xyz.chunkstories.graphics.opengl.OpenglGraphicsBackend
import java.nio.ByteBuffer

class OpenglUniformBuffer(val backend: OpenglGraphicsBackend, val mapper: GLSLType.JvmStruct) : Cleanable {
    internal val glId: Int

    init {
        glId = glCreateBuffers()
    }

    fun upload(data: ByteBuffer) {
        glNamedBufferData(glId, data, GL_STREAM_DRAW)
    }

    override fun cleanup() {
        glDeleteBuffers(glId)
    }
}