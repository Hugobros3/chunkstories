package xyz.chunkstories.graphics.opengl.textures

import xyz.chunkstories.api.graphics.Texture2D
import xyz.chunkstories.api.graphics.TextureFormat
import xyz.chunkstories.graphics.opengl.OpenglGraphicsBackend

import org.lwjgl.opengl.ARBDirectStateAccess.*
import org.lwjgl.opengl.GL30.*
import java.nio.ByteBuffer

class OpenglTexture2D(backend: OpenglGraphicsBackend, format: TextureFormat,
                      override val width: Int, override val height: Int) : OpenglTexture(backend, format, GL_TEXTURE_2D), Texture2D {

    init {
        if(backend.openglSupport.dsaSupport) {
            glTextureStorage2D(glTexId, 1, format.glMapping.internalFormat, width, height)
            glTextureParameteri(glTexId, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
            glTextureParameteri(glTexId, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        } else {
            glBindTexture(GL_TEXTURE_2D, glTexId)
            glTexImage2D(GL_TEXTURE_2D, 0, format.glMapping.internalFormat, width, height, 0, format.glMapping.format, GL_UNSIGNED_BYTE, null as ByteBuffer?)
        }
    }

    fun upload(buffer: ByteBuffer) {
        if(backend.openglSupport.dsaSupport) {
            glTextureSubImage2D(glTexId, 0, 0, 0, width, height, format.glMapping.format, GL_UNSIGNED_BYTE, buffer)
        } else {
            val t = glGetInteger(GL_TEXTURE_BINDING_2D)

            glBindTexture(GL_TEXTURE_2D, glTexId)
            glTexImage2D(GL_TEXTURE_2D, 0, format.glMapping.internalFormat, width, height, 0, format.glMapping.format, GL_UNSIGNED_BYTE, buffer)

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)

            glBindTexture(GL_TEXTURE_2D, t)
        }
    }
}