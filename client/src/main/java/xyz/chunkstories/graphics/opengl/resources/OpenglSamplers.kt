package xyz.chunkstories.graphics.opengl.resources

import xyz.chunkstories.api.graphics.TextureTilingMode
import xyz.chunkstories.api.graphics.rendergraph.ImageInput
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.opengl.OpenglGraphicsBackend
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class OpenglSamplers(val backend: OpenglGraphicsBackend) : Cleanable {
    private data class Index(val tilingMode: TextureTilingMode, val mipmapping: Boolean, val depthCompareMode: ImageInput.DepthCompareMode, val scalingMode: ImageInput.ScalingMode)

    private val pool = mutableMapOf<Index, OpenglSampler>()
    private val lock = ReentrantLock()

    fun getSamplerForImageInputParameters(imageInput: ImageInput) : OpenglSampler {
        val index = Index(imageInput.tilingMode, imageInput.mipmapping, imageInput.depthCompareMode, imageInput.scalingMode)
        return lock.withLock {
            pool.getOrPut(index) {
                OpenglSampler(backend, index.scalingMode, index.depthCompareMode, index.tilingMode)
            }
        }
    }

    override fun cleanup() {
        pool.values.forEach(Cleanable::cleanup)
    }
}