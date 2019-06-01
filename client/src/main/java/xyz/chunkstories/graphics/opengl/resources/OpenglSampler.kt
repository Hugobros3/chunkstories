package xyz.chunkstories.graphics.opengl.resources

import org.lwjgl.opengl.GL33.*
import org.lwjgl.opengl.ARBDirectStateAccess.*
import org.lwjgl.opengl.GL11
import xyz.chunkstories.api.graphics.TextureTilingMode
import xyz.chunkstories.api.graphics.rendergraph.ImageInput
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.opengl.OpenglGraphicsBackend

class OpenglSampler(val backend: OpenglGraphicsBackend, val scalingMode: ImageInput.ScalingMode, val depthCompareMode: ImageInput.DepthCompareMode, val tilingMode: TextureTilingMode) : Cleanable {

    val glId: Int

    init {
        if(backend.openglSupport.dsaSupport) {
            glId = glCreateSamplers()
        } else {
            glId = glGenSamplers()
            //val t = GL11.glGetInteger(GL_SAMPLER_BINDING)
            glBindSampler(0, glId)
        }

        when(scalingMode) {
            ImageInput.ScalingMode.LINEAR -> {
                glSamplerParameteri(glId, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
                glSamplerParameteri(glId, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            }
            ImageInput.ScalingMode.NEAREST -> {
                glSamplerParameteri(glId, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
                glSamplerParameteri(glId, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
            }
        }

        when(depthCompareMode) {
            ImageInput.DepthCompareMode.DISABLED -> {
                glSamplerParameteri(glId, GL_TEXTURE_COMPARE_MODE, GL_NONE)
            }
            ImageInput.DepthCompareMode.GREATER -> {
                glSamplerParameteri(glId, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_R_TO_TEXTURE)
                glSamplerParameteri(glId, GL_TEXTURE_COMPARE_FUNC, GL_GREATER)
            }
            ImageInput.DepthCompareMode.GREATER_OR_EQUAL -> {
                glSamplerParameteri(glId, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_R_TO_TEXTURE)
                glSamplerParameteri(glId, GL_TEXTURE_COMPARE_FUNC, GL_GEQUAL)
            }
            ImageInput.DepthCompareMode.EQUAL -> {
                glSamplerParameteri(glId, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_R_TO_TEXTURE)
                glSamplerParameteri(glId, GL_TEXTURE_COMPARE_FUNC, GL_EQUAL)
            }
            ImageInput.DepthCompareMode.LESS_OR_EQUAL -> {
                glSamplerParameteri(glId, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_R_TO_TEXTURE)
                glSamplerParameteri(glId, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL)
            }
            ImageInput.DepthCompareMode.LESS -> {
                glSamplerParameteri(glId, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_R_TO_TEXTURE)
                glSamplerParameteri(glId, GL_TEXTURE_COMPARE_FUNC, GL_LESS)
            }
        }

        when(tilingMode) {
            TextureTilingMode.CLAMP_TO_EDGE -> {
                glSamplerParameteri(glId, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
                glSamplerParameteri(glId, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
                glSamplerParameteri(glId, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE)
            }
            TextureTilingMode.REPEAT -> {
                glSamplerParameteri(glId, GL_TEXTURE_WRAP_S, GL_REPEAT)
                glSamplerParameteri(glId, GL_TEXTURE_WRAP_T, GL_REPEAT)
                glSamplerParameteri(glId, GL_TEXTURE_WRAP_R, GL_REPEAT)
            }
        }
    }

    override fun cleanup() {
        glDeleteSamplers(glId)
    }
}