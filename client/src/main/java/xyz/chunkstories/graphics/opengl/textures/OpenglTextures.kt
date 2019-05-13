package xyz.chunkstories.graphics.opengl.textures

import de.matthiasmann.twl.utils.PNGDecoder
import org.lwjgl.system.MemoryStack.stackPop
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.memAlloc
import org.lwjgl.system.MemoryUtil.memFree
import xyz.chunkstories.api.graphics.GraphicsEngine
import xyz.chunkstories.api.graphics.TextureFormat
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.opengl.OpenglGraphicsBackend
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class OpenglTextures(val backend: OpenglGraphicsBackend) : GraphicsEngine.Textures, Cleanable {

    val lock = ReentrantLock()
    val loadedTexture2D = mutableMapOf<String, OpenglTexture2D>()
    val loadedCubemaps = mutableMapOf<String, OpenglTextureCubemap>()

    override val defaultTexture2D: OpenglTexture2D
        get() = getOrLoadTexture2D("textures/notex.png")

    override fun getOrLoadTexture2D(assetName: String): OpenglTexture2D {
        try {
            lock.lock()

            return loadedTexture2D.getOrPut(assetName) {
                val asset = backend.window.client.content.getAsset(assetName) ?: return defaultTexture2D

                stackPush()
                // TODO use STBI instead ?
                val decoder = PNGDecoder(asset.read())
                val width = decoder.width
                val height = decoder.height
                val buffer = memAlloc(4 * width * height)
                decoder.decode(buffer, width * 4, PNGDecoder.Format.RGBA)
                buffer.flip()

                val format = TextureFormat.RGBA_8 // TODO when adding support for HDR change that as well
                val texture2D = OpenglTexture2D(backend, format, width, height)

                texture2D.upload(buffer)

                memFree(buffer)
                stackPop()

                texture2D
            }
        } finally {
            lock.unlock()
        }
    }

    fun getOrLoadCubemap(assetName: String): OpenglTextureCubemap {
        try {
            lock.lock()

            return loadedCubemaps.getOrPut(assetName) {
                val faces = listOf("right", "left", "top", "bottom", "front", "back")

                var width = 0
                var height = 0
                var alloc = lazy {
                    Array(6) { memAlloc(width * height * 4) }
                }
                for ((i, face) in faces.withIndex()) {
                    val asset = backend.window.client.content.getAsset("$assetName$face.png")!!
                    val decoder = PNGDecoder(asset.read())
                    width = decoder.width
                    height = decoder.height
                    decoder.decode(alloc.value[i], width * 4, PNGDecoder.Format.RGBA)
                    alloc.value[i].flip()
                }

                val cubemap = OpenglTextureCubemap(backend, TextureFormat.RGBA_8, width, height)
                for(i in 0..5) {
                    cubemap.upload(alloc.value[i], i)
                }

                alloc.value.forEach { memFree(it) }
                cubemap

            }
        } finally {
            lock.unlock()
        }
    }

    override fun cleanup() {
        lock.withLock {
            loadedTexture2D.values.forEach(Cleanable::cleanup)
            loadedTexture2D.clear()
        }
    }
}