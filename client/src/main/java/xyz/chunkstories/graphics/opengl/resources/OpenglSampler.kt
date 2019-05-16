package xyz.chunkstories.graphics.opengl.resources

import org.lwjgl.opengl.GL33.*
import org.lwjgl.opengl.ARBDirectStateAccess.*
import xyz.chunkstories.api.graphics.TextureTilingMode
import xyz.chunkstories.api.graphics.rendergraph.ImageInput
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.opengl.OpenglGraphicsBackend


class OpenglSampler(val backend: OpenglGraphicsBackend, val scalingMode: ImageInput.ScalingMode, val depthCompareMode: ImageInput.DepthCompareMode, val tilingMode: TextureTilingMode) : Cleanable {

    val glId: Int

    init {
        glId = glCreateSamplers()

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
            ImageInput.DepthCompareMode.SHADOWMAP -> {
                glSamplerParameteri(glId, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_R_TO_TEXTURE)
                glSamplerParameteri(glId, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL)
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