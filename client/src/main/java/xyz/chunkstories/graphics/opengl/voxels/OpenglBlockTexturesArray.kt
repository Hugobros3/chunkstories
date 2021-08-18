package xyz.chunkstories.graphics.opengl.voxels

import org.lwjgl.system.MemoryUtil.memAlloc
import org.lwjgl.system.MemoryUtil.memFree
import xyz.chunkstories.api.graphics.TextureFormat
import xyz.chunkstories.block.BlockTypesStore
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.common.util.toByteBuffer
import xyz.chunkstories.graphics.common.voxel.BlockTexturesOnion
import xyz.chunkstories.graphics.opengl.OpenglGraphicsBackend
import xyz.chunkstories.graphics.opengl.textures.OpenglOnionTexture2D
import java.awt.image.BufferedImage

class OpenglBlockTexturesArray(val backend: OpenglGraphicsBackend, blockTypesStore: BlockTypesStore) : BlockTexturesOnion(blockTypesStore), Cleanable {
    lateinit var albedoOnionTexture: OpenglOnionTexture2D

    override fun createTextureArray(textureResolution: Int, imageData: List<Array<BufferedImage>>) {
        backend.window.mainThreadBlocking {
            if (::albedoOnionTexture.isInitialized)
                albedoOnionTexture.cleanup()

            albedoOnionTexture = OpenglOnionTexture2D(backend, TextureFormat.RGBA_8, textureResolution, textureResolution, imageData.size)

            for ((i, data) in imageData.withIndex()) {
                val buffer = memAlloc(4 * textureResolution * textureResolution)
                buffer.put(data[0].toByteBuffer())
                buffer.flip()
                albedoOnionTexture.upload(buffer, i)
                memFree(buffer)
            }
        }
    }

    override fun cleanup() {
        albedoOnionTexture.cleanup()
    }
}