package xyz.chunkstories.graphics.vulkan.resources

import xyz.chunkstories.api.graphics.TextureTilingMode
import xyz.chunkstories.api.graphics.rendergraph.ImageInput
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.textures.VulkanSampler
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class VulkanSamplers(val backend: VulkanGraphicsBackend) : Cleanable {
    private data class Index(val tilingMode: TextureTilingMode, val mipmapping: Boolean, val depthCompareMode: ImageInput.DepthCompareMode, val scalingMode: ImageInput.ScalingMode)
    private val pool = mutableMapOf<Index, VulkanSampler>()
    private val lock = ReentrantLock()

    fun getSamplerForImageInputParameters(imageInput: ImageInput) : VulkanSampler {
        val index = Index(imageInput.tilingMode, imageInput.mipmapping, imageInput.depthCompareMode, imageInput.scalingMode)
        return lock.withLock {
            pool.getOrPut(index) {
                VulkanSampler(backend, index.scalingMode, index.depthCompareMode, index.tilingMode)
            }
        }
    }

    override fun cleanup() {
        pool.values.forEach(Cleanable::cleanup)
    }
}