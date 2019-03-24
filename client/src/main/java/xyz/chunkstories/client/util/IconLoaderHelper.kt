//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.client.util

import de.matthiasmann.twl.utils.PNGDecoder
import de.matthiasmann.twl.utils.PNGDecoder.Format
import org.lwjgl.glfw.GLFW.glfwSetWindowIcon
import org.lwjgl.glfw.GLFWImage
import xyz.chunkstories.client.glfw.GLFWWindow
import xyz.chunkstories.client.util.IconLoaderHelper.getByteBufferData
import java.nio.ByteBuffer

fun GLFWWindow.loadIcons() {
    val icons = GLFWImage.malloc(3)

    val pixels16 = getByteBufferData("/textures/icon16.png")
    icons.position(0).width(16).height(16).pixels(pixels16!!)

    val pixels32 = getByteBufferData("/textures/icon32.png")
    icons.position(1).width(32).height(32).pixels(pixels32!!)

    val pixels128 = getByteBufferData("/textures/icon128.png")
    icons.position(2).width(128).height(128).pixels(pixels128!!)

    icons.position(0)

    glfwSetWindowIcon(this.glfwWindowHandle, icons)

    icons.free()
}

object IconLoaderHelper {
    internal fun getByteBufferData(name: String): ByteBuffer? {
        val decoder = PNGDecoder(javaClass.getResourceAsStream(name))
        val width = decoder.width
        val height = decoder.height
        val temp = ByteBuffer.allocateDirect(4 * width * height)
        decoder.decode(temp, width * 4, Format.RGBA)
        temp.flip()
        return temp
    }
}
