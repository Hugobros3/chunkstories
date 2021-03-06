package xyz.chunkstories.graphics.opengl.textures

import org.lwjgl.opengl.ARBDirectStateAccess.*
import org.lwjgl.opengl.GL11.glDeleteTextures
import org.lwjgl.opengl.GL11.glGenTextures
import xyz.chunkstories.api.graphics.Texture
import xyz.chunkstories.api.graphics.TextureFormat
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.opengl.OpenglGraphicsBackend

abstract class OpenglTexture(val backend: OpenglGraphicsBackend, final override val format: TextureFormat, type: Int) : Texture, Cleanable {
    val glTexId: Int

    init {
        if(backend.openglSupport.dsaSupport) {
            glTexId = glCreateTextures(type)
        } else {
            glTexId = glGenTextures()
        }
    }

    override fun cleanup() {
        glDeleteTextures(glTexId)
    }
}