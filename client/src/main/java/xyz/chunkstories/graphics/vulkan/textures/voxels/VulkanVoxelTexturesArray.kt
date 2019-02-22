package xyz.chunkstories.graphics.vulkan.textures.voxels

import de.matthiasmann.twl.utils.PNGDecoder
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK10.*
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.graphics.TextureFormat
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.buffers.VulkanBuffer
import xyz.chunkstories.graphics.vulkan.memory.MemoryUsagePattern
import xyz.chunkstories.graphics.vulkan.resources.Cleanable
import xyz.chunkstories.graphics.vulkan.textures.VulkanOnionTexture2D
import xyz.chunkstories.graphics.vulkan.textures.VulkanTexture2D
import xyz.chunkstories.util.toByteBuffer
import java.awt.image.BufferedImage

class VulkanVoxelTexturesArray(val backend: VulkanGraphicsBackend, voxels: Content.Voxels) : VoxelTexturesArray(voxels), Cleanable {
    lateinit var albedoOnionTexture: VulkanOnionTexture2D

    override fun createTextureArray(textureResolution: Int, imageData: List<Array<BufferedImage>>) {
        if(::albedoOnionTexture.isInitialized)
            albedoOnionTexture.cleanup()

        MemoryStack.stackPush()

        val layerCount = imageData.size
        val buffer = MemoryUtil.memAlloc(4 * textureResolution * textureResolution * layerCount)
        for(data in imageData) {
            buffer.put(data[0].toByteBuffer())
        }
        buffer.flip()

        val vkBuffer = VulkanBuffer(backend, buffer, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, MemoryUsagePattern.STAGING)

        val format = TextureFormat.RGBA_8 // TODO when adding support for HDR change that as well

        albedoOnionTexture = VulkanOnionTexture2D(backend, format, textureResolution, textureResolution, layerCount, VK_IMAGE_USAGE_TRANSFER_DST_BIT or VK_IMAGE_USAGE_SAMPLED_BIT)

        albedoOnionTexture.transitionLayout(VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
        albedoOnionTexture.copyBufferToImage(vkBuffer)
        albedoOnionTexture.transitionLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)

        MemoryUtil.memFree(buffer)
        MemoryStack.stackPop()
    }

    override fun cleanup() {
        albedoOnionTexture.cleanup()
    }
}