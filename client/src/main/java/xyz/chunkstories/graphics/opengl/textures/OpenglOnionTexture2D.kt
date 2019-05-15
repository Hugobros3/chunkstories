package xyz.chunkstories.graphics.opengl.textures

import xyz.chunkstories.api.graphics.TextureFormat
import xyz.chunkstories.graphics.opengl.OpenglGraphicsBackend

import org.lwjgl.opengl.ARBDirectStateAccess.*
import org.lwjgl.opengl.GL30.*
import java.nio.ByteBuffer

class OpenglOnionTexture2D(backend: OpenglGraphicsBackend, format: TextureFormat, val width: Int, val height: Int, val layerCount: Int) : OpenglTexture(backend, format, GL_TEXTURE_2D_ARRAY) {
    init {
        glTextureStorage3D(glTexId, 1, format.glMapping.internalFormat, width, height, layerCount)
        glTextureParameteri(glTexId, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTextureParameteri(glTexId, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
    }

    fun upload(buffer: ByteBuffer, index: Int) {
        // this is poo DSA >:(
        glTextureSubImage3D(glTexId, 0, 0, 0, index, width, height, 1, format.glMapping.format, GL_UNSIGNED_BYTE, buffer)
    }
}