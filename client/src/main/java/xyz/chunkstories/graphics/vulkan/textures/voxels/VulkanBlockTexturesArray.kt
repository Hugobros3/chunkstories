package xyz.chunkstories.graphics.vulkan.textures.voxels

import org.lwjgl.system.MemoryStack.stackPop
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.memAlloc
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.VK10.*
import xyz.chunkstories.api.graphics.TextureFormat
import xyz.chunkstories.block.BlockTypesStore
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.buffers.VulkanBuffer
import xyz.chunkstories.graphics.vulkan.memory.MemoryUsagePattern
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.common.voxel.BlockTexturesOnion
import xyz.chunkstories.graphics.vulkan.textures.VulkanOnionTexture2D
import xyz.chunkstories.util.toByteBuffer
import java.awt.image.BufferedImage

class VulkanBlockTexturesArray(val backend: VulkanGraphicsBackend, blockTypes: BlockTypesStore) : BlockTexturesOnion(blockTypes), Cleanable {
    lateinit var albedoOnionTexture: VulkanOnionTexture2D

    override fun createTextureArray(textureResolution: Int, imageData: List<Array<BufferedImage>>) {
        if(::albedoOnionTexture.isInitialized)
            albedoOnionTexture.cleanup()

        stackPush()

        val layerCount = imageData.size
        val buffer = memAlloc(4 * textureResolution * textureResolution * layerCount)
        for(data in imageData) {
            buffer.put(data[0].toByteBuffer())
        }
        buffer.flip()

        val stagingBuffer = VulkanBuffer(backend, buffer, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, MemoryUsagePattern.STAGING)

        val format = TextureFormat.RGBA_8 // TODO when adding support for HDR change that as well

        albedoOnionTexture = VulkanOnionTexture2D(backend, format, textureResolution, textureResolution, layerCount, VK_IMAGE_USAGE_TRANSFER_DST_BIT or VK_IMAGE_USAGE_SAMPLED_BIT)

        albedoOnionTexture.transitionLayout(VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
        albedoOnionTexture.copyBufferToImage(stagingBuffer)
        albedoOnionTexture.transitionLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)

        stagingBuffer.cleanup()
        memFree(buffer)
        stackPop()
    }

    override fun cleanup() {
        albedoOnionTexture.cleanup()
    }
}