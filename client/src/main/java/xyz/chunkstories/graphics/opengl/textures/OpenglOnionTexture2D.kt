package xyz.chunkstories.graphics.opengl.textures

import xyz.chunkstories.api.graphics.TextureFormat
import xyz.chunkstories.graphics.opengl.OpenglGraphicsBackend

import org.lwjgl.opengl.ARBDirectStateAccess.*
import org.lwjgl.opengl.GL12
import org.lwjgl.opengl.GL30.*
import java.nio.ByteBuffer

class OpenglOnionTexture2D(backend: OpenglGraphicsBackend, format: TextureFormat, val width: Int, val height: Int, val layerCount: Int) : OpenglTexture(backend, format, GL_TEXTURE_2D_ARRAY) {
    init {
        if(backend.openglSupport.dsaSupport) {
            glTextureStorage3D(glTexId, 1, format.glMapping.internalFormat, width, height, layerCount)
            glTextureParameteri(glTexId, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
            glTextureParameteri(glTexId, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        } else {
            glBindTexture(GL_TEXTURE_2D_ARRAY, glTexId)
            glTexImage3D(GL_TEXTURE_2D_ARRAY, 0, format.glMapping.internalFormat, width, height, layerCount, 0, format.glMapping.format, GL_UNSIGNED_BYTE, null as ByteBuffer?)
        }
    }

    fun upload(buffer: ByteBuffer, index: Int) {
        if(backend.openglSupport.dsaSupport) {
            glTextureSubImage3D(glTexId, 0, 0, 0, index, width, height, 1, format.glMapping.format, GL_UNSIGNED_BYTE, buffer)
        }else {
            val t = glGetInteger(GL_TEXTURE_BINDING_2D_ARRAY)

            glBindTexture(GL_TEXTURE_2D_ARRAY, glTexId)
            glTexSubImage3D(GL_TEXTURE_2D_ARRAY, 0, 0,0, index, width, height, 1, format.glMapping.format, GL_UNSIGNED_BYTE, buffer)
            //glTexImage3D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + face, 0, format.glMapping.internalFormat, width, height, 0, format.glMapping.format, GL_UNSIGNED_BYTE, buffer)

            glTextureParameteri(glTexId, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
            glTextureParameteri(glTexId, GL_TEXTURE_MAG_FILTER, GL_NEAREST)

            glBindTexture(GL_TEXTURE_2D_ARRAY, t)
        }
    }
}