package xyz.chunkstories.graphics.opengl.textures

import org.lwjgl.opengl.ARBDirectStateAccess
import xyz.chunkstories.api.graphics.Texture2D
import xyz.chunkstories.api.graphics.TextureFormat
import xyz.chunkstories.graphics.opengl.OpenglGraphicsBackend

import org.lwjgl.opengl.ARBDirectStateAccess.*
import org.lwjgl.opengl.GL30.*
import java.nio.ByteBuffer

class OpenglTexture2D(backend: OpenglGraphicsBackend, format: TextureFormat,
                      override val width: Int, override val height: Int) : OpenglTexture(backend, format, GL_TEXTURE_2D), Texture2D {

    init {
        glTextureStorage2D(glTexId, 1, format.glMapping.internalFormat, width, height)
    }

    fun upload(buffer: ByteBuffer) {
        ARBDirectStateAccess.glTextureSubImage2D(glTexId, 0, 0, 0, width, height, format.glMapping.format, GL_UNSIGNED_BYTE, buffer)
    }
}