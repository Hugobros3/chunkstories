package xyz.chunkstories.graphics.vulkan.textures

import de.matthiasmann.twl.utils.PNGDecoder
import xyz.chunkstories.api.graphics.GraphicsEngine
import xyz.chunkstories.api.graphics.Texture2D
import xyz.chunkstories.api.graphics.TextureFormat
import xyz.chunkstories.graphics.vulkan.CommandPool
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.buffers.VulkanBuffer
import xyz.chunkstories.graphics.vulkan.resources.Cleanable
import xyz.chunkstories.graphics.vulkan.util.VulkanFormat
import org.lwjgl.system.MemoryStack.stackPop
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.memAlloc
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.VK10.*
import xyz.chunkstories.graphics.vulkan.memory.MemoryUsagePattern
import xyz.chunkstories.graphics.vulkan.util.updateDescriptorSet
import java.util.concurrent.locks.ReentrantLock

class VulkanTextures(val backend: VulkanGraphicsBackend) : GraphicsEngine.Textures, Cleanable {

    val lock = ReentrantLock()
    val commandPool: CommandPool
    val loadedTextures2D = mutableMapOf<String, VulkanTexture2D>()

    val magicTexturing: MagicTexturing?

    init {
        commandPool = CommandPool(backend, backend.logicalDevice.graphicsQueue.family, VK_COMMAND_POOL_CREATE_TRANSIENT_BIT or VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
        if(backend.logicalDevice.enableMagicTexturing)
            magicTexturing = MagicTexturing(backend)
        else
            magicTexturing = null
    }

    override val defaultTexture2D: Texture2D
        get() = getOrLoadTexture2D("textures/notex.png")

    override fun getOrLoadTexture2D(assetName: String): Texture2D {
        try {
            lock.lock()

            return loadedTextures2D.getOrPut(assetName) {
                val asset = backend.window.client.content.getAsset(assetName) ?: return defaultTexture2D

                stackPush()
                // TODO use STBI instead ?
                val decoder = PNGDecoder(asset.read())
                val width = decoder.width
                val height = decoder.height
                val buffer = memAlloc(4 * width * height)
                decoder.decode(buffer, width * 4, PNGDecoder.Format.RGBA)
                buffer.flip()

                val vkBuffer = VulkanBuffer(backend, buffer, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, MemoryUsagePattern.STAGING)

                val format = TextureFormat.RGBA_8 // TODO when adding support for HDR change that as well
                val texture2D = VulkanTexture2D(backend, format, width, height, VK_IMAGE_USAGE_TRANSFER_DST_BIT or VK_IMAGE_USAGE_SAMPLED_BIT)

                texture2D.transitionLayout(VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                texture2D.copyBufferToImage(vkBuffer)
                texture2D.transitionLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)

                memFree(buffer)
                stackPop()

                vkBuffer.cleanup()

                texture2D
            }
        } finally {
            lock.unlock()
        }
    }

    fun dropLoadedTextures() {
        loadedTextures2D.values.toSet().forEach { it.cleanup() }
    }

    override fun cleanup() {
        dropLoadedTextures()
        commandPool.cleanup()
        magicTexturing?.cleanup()
    }
}

//TODO look those up at logical device init time
val TextureFormat.vulkanFormat: VulkanFormat
    get() = when (this) {
        TextureFormat.RGBA_8 -> VulkanFormat.VK_FORMAT_R8G8B8A8_UNORM
        TextureFormat.RGB_HDR -> VulkanFormat.VK_FORMAT_R16G16B16A16_SFLOAT
        TextureFormat.DEPTH_32 -> VulkanFormat.VK_FORMAT_D32_SFLOAT
        TextureFormat.RGB_8 -> TODO()
        TextureFormat.RG_8 -> TODO()
        TextureFormat.RED_8 -> TODO()
        TextureFormat.DEPTH_24 -> TODO()
        TextureFormat.RED_32F -> TODO()
        TextureFormat.RED_16I -> TODO()
        TextureFormat.RED_16F -> TODO()
        TextureFormat.RGBA_3x10_2 -> TODO()
        TextureFormat.RGBA_16F -> TODO()
        TextureFormat.RGBA_32F -> TODO()
        TextureFormat.RED_8UI -> TODO()
        else -> throw Exception("Unmapped texture format $this")
    }