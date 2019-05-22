package xyz.chunkstories.graphics.opengl.textures

import xyz.chunkstories.api.graphics.TextureFormat
import xyz.chunkstories.graphics.opengl.OpenglGraphicsBackend

import org.lwjgl.opengl.ARBDirectStateAccess.*
import org.lwjgl.opengl.GL30.*
import java.nio.ByteBuffer

class OpenglTextureCubemap(backend: OpenglGraphicsBackend, format: TextureFormat, val width: Int, val height: Int) : OpenglTexture(backend, format, GL_TEXTURE_CUBE_MAP) {
    init {
        if(backend.openglSupport.dsaSupport) {
            glTextureStorage2D(glTexId, 1, format.glMapping.internalFormat, width, height)

            glTextureParameteri(glTexId, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
            glTextureParameteri(glTexId, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        }
    }

    fun upload(buffer: ByteBuffer, face: Int) {
        if(backend.openglSupport.dsaSupport) {
            // this is poo DSA >:(
            glTextureSubImage3D(glTexId, 0, 0, 0, face, width, height, 1, format.glMapping.format, GL_UNSIGNED_BYTE, buffer)
        } else {
            val t = glGetInteger(GL_TEXTURE_BINDING_CUBE_MAP)

            glBindTexture(GL_TEXTURE_CUBE_MAP, glTexId)
            glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + face, 0, format.glMapping.internalFormat, width, height, 0, format.glMapping.format, GL_UNSIGNED_BYTE, buffer)

            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_NEAREST)

            glBindTexture(GL_TEXTURE_CUBE_MAP, t)
        }
    }
}