package xyz.chunkstories.graphics.opengl.textures

import xyz.chunkstories.api.graphics.TextureFormat

import org.lwjgl.opengl.GL30.*

/** OpenGL treats texture formats in this annoying triplets manner so here's a data class to hold them */
data class GLTextureFormatValues(val internalFormat: Int, val format: Int, val type: Int)

val TextureFormat.glMapping
    get() = when (this) {
        TextureFormat.RGBA_8 -> GLTextureFormatValues(GL_RGBA, GL_RGBA, GL_UNSIGNED_BYTE)
        TextureFormat.RGB_8 -> GLTextureFormatValues(GL_RGB8, GL_RGB, GL_UNSIGNED_BYTE)
        TextureFormat.RG_8 -> GLTextureFormatValues(GL_RG8, GL_RG, GL_UNSIGNED_BYTE)
        TextureFormat.RED_8 -> GLTextureFormatValues(GL_R8, GL_RG, GL_UNSIGNED_BYTE)
        TextureFormat.RGB_HDR -> GLTextureFormatValues(GL_R11F_G11F_B10F, GL_RGB, GL_FLOAT)
        TextureFormat.DEPTH_32 -> GLTextureFormatValues(GL_DEPTH_COMPONENT32, GL_DEPTH_COMPONENT, GL_FLOAT)
        TextureFormat.DEPTH_24 -> GLTextureFormatValues(GL_DEPTH_COMPONENT24, GL_DEPTH_COMPONENT, GL_FLOAT)
        TextureFormat.RED_32F -> GLTextureFormatValues(GL_R32F, GL_RED, GL_FLOAT)
        TextureFormat.RED_16I -> GLTextureFormatValues(GL_R16UI, GL_RED_INTEGER, GL_INT)
        TextureFormat.RED_16F -> GLTextureFormatValues(GL_R16F, GL_RED, GL_FLOAT)
        TextureFormat.RGBA_3x10_2 -> GLTextureFormatValues(GL_RGB10_A2, GL_RGBA, GL_UNSIGNED_BYTE)
        TextureFormat.RGBA_16F -> GLTextureFormatValues(GL_RGBA16F, GL_RGBA, GL_FLOAT)
        TextureFormat.RGBA_32F -> GLTextureFormatValues(GL_RGBA32F, GL_RGBA, GL_FLOAT)
        TextureFormat.RED_8UI -> GLTextureFormatValues(GL_R8UI, GL_RED_INTEGER, GL_INT)
    }