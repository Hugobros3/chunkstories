package xyz.chunkstories.graphics.opengl.textures

import xyz.chunkstories.api.graphics.TextureFormat
import xyz.chunkstories.graphics.opengl.OpenglGraphicsBackend

import org.lwjgl.opengl.ARBDirectStateAccess.*
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL30.*
import java.nio.ByteBuffer

class OpenglTextureCubemap(backend: OpenglGraphicsBackend, format: TextureFormat, val width: Int, val height: Int) : OpenglTexture(backend, format, GL13.GL_TEXTURE_CUBE_MAP) {
    init {
        glTextureStorage2D(glTexId, 1, format.glMapping.internalFormat, width, height)
        glTextureParameteri(glTexId, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTextureParameteri(glTexId, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
    }

    fun upload(buffer: ByteBuffer, face: Int) {
        // this is poo DSA >:(
        glTextureSubImage3D(glTexId, 0, 0, 0, face, width, height, 1, format.glMapping.format, GL_UNSIGNED_BYTE, buffer)
    }
}