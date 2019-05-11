package xyz.chunkstories.graphics.opengl.textures

import xyz.chunkstories.api.graphics.Texture2D
import xyz.chunkstories.api.graphics.TextureFormat
import xyz.chunkstories.graphics.opengl.OpenglGraphicsBackend

import org.lwjgl.opengl.ARBDirectStateAccess.*
import org.lwjgl.opengl.GL30.*

class OpenglTexture2D(backend: OpenglGraphicsBackend, format: TextureFormat,
                      override val width: Int, override val height: Int) : OpenglTexture(backend, format, GL_TEXTURE_2D), Texture2D {

    init {
        glTextureStorage2D(glTexId, 1, format.glMapping.internalFormat, width, height)
    }

}