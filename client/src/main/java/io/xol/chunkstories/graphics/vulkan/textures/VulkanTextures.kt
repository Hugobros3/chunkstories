package io.xol.chunkstories.graphics.vulkan.textures

import de.matthiasmann.twl.utils.PNGDecoder
import io.xol.chunkstories.api.content.Content
import io.xol.chunkstories.api.graphics.GraphicsEngine
import io.xol.chunkstories.api.graphics.Texture2D
import io.xol.chunkstories.api.graphics.TextureFormat
import io.xol.chunkstories.graphics.vulkan.CommandPool
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.buffers.VulkanBuffer
import io.xol.chunkstories.graphics.vulkan.resources.Cleanable
import io.xol.chunkstories.graphics.vulkan.util.VulkanFormat
import org.lwjgl.system.MemoryStack.stackPop
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.memAlloc
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.VK10.*

class VulkanTextures(val backend: VulkanGraphicsBackend) : GraphicsEngine.Textures, Cleanable {

    val commandPool: CommandPool
    val loadedTextures2D = mutableMapOf<String, VulkanTexture2D>()

    init {
        commandPool = CommandPool(backend, backend.logicalDevice.graphicsQueue.family, VK_COMMAND_POOL_CREATE_TRANSIENT_BIT or VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
    }

    override val defaultTexture2D: Texture2D
        get() = getOrLoadTexture2D("textures/notex.png")

    override fun getOrLoadTexture2D(assetName: String): Texture2D = loadedTextures2D.getOrPut(assetName) {
        val asset = backend.window.client.content.getAsset(assetName) ?: return defaultTexture2D

        stackPush()
        // TODO use STBI instead ?
        val decoder = PNGDecoder(asset.read())
        val width = decoder.width
        val height = decoder.height
        val buffer = memAlloc(4 * width * height)
        decoder.decode(buffer, width * 4, PNGDecoder.Format.RGBA)
        buffer.flip()

        val vkBuffer = VulkanBuffer(backend, buffer, VK_BUFFER_USAGE_TRANSFER_SRC_BIT)

        val format = TextureFormat.RGBA_8 // TODO when adding support for HDR change that as well
        val texture2D = VulkanTexture2D(backend, this, format, width, height, VK_IMAGE_USAGE_TRANSFER_SRC_BIT or VK_IMAGE_USAGE_SAMPLED_BIT)

        texture2D.transitionLayout(VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
        texture2D.copyBufferToImage(vkBuffer)
        texture2D.transitionLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)

        memFree(buffer)
        stackPop()

        vkBuffer.cleanup()

        texture2D
    }

    fun dropLoadedTextures() {
        loadedTextures2D.values.toSet().forEach { it.cleanup() }
    }

    override fun cleanup() {
        dropLoadedTextures()
        commandPool.cleanup()
    }
}

val TextureFormat.vulkanFormat: VulkanFormat
    get() = when (this) {
        TextureFormat.RGBA_8 -> VulkanFormat.VK_FORMAT_R8G8B8A8_UNORM
        else -> throw Exception("Unmapped texture format $this")
    }