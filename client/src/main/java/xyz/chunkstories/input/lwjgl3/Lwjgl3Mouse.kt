//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.input.lwjgl3

import org.lwjgl.glfw.GLFW.GLFW_CURSOR
import org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED
import org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL
import org.lwjgl.glfw.GLFW.glfwGetCursorPos
import org.lwjgl.glfw.GLFW.glfwGetInputMode
import org.lwjgl.glfw.GLFW.glfwSetCursorPos
import org.lwjgl.glfw.GLFW.glfwSetInputMode

import java.nio.DoubleBuffer

import xyz.chunkstories.api.client.Client
import xyz.chunkstories.client.ClientImplementation
import org.joml.Vector2d
import org.lwjgl.system.MemoryUtil

import xyz.chunkstories.api.input.Input
import xyz.chunkstories.api.input.Mouse

class Lwjgl3Mouse(internal var im: Lwjgl3ClientInputsManager) : Mouse {

    val mousePosition: Vector2d
        get() {
            val b1 = MemoryUtil.memAllocDouble(1)
            val b2 = MemoryUtil.memAllocDouble(1)
            glfwGetCursorPos(im.gameWindow.glfwWindowHandle, b1, b2)
            val vec2 = Vector2d(b1.get(), im.gameWindow.height - b2.get())
            MemoryUtil.memFree(b1)
            MemoryUtil.memFree(b2)

            return vec2
        }

    override val mainButton: Mouse.MouseButton
        get() = im.LEFT

    override val secondaryButton: Mouse.MouseButton
        get() = im.RIGHT

    override val middleButton: Mouse.MouseButton
        get() = im.MIDDLE

    override val cursorX: Double
        get(): Double {
            return mousePosition.x()
        }

    override val cursorY: Double
        get(): Double {
            return mousePosition.y()
        }

    override fun setMouseCursorLocation(x: Double, y: Double) {
        glfwSetCursorPos(im.gameWindow.glfwWindowHandle, x, y)
    }

    override var isGrabbed: Boolean
        get(): Boolean {
            return glfwGetInputMode(im.gameWindow.glfwWindowHandle, GLFW_CURSOR) == GLFW_CURSOR_DISABLED
        }
        set(grabbed) {
            glfwSetInputMode(this.im.gameWindow.glfwWindowHandle, GLFW_CURSOR,
                    if (grabbed) GLFW_CURSOR_DISABLED else GLFW_CURSOR_NORMAL)
        }

    fun scroll(yoffset: Double): Mouse.MouseScroll {
        return object : Mouse.MouseScroll {

            override val client: Client
                get() = im.gameWindow.client

            override val name: String
                get() = "mouse.scroll"

            override val isPressed: Boolean
                get() = false

            override val hash: Long
                get() {
                    throw UnsupportedOperationException("MouseScroll.getHash()")
                }

            override fun amount(): Int {
                return yoffset.toInt()
            }

            override fun equals(o: Any?): Boolean {
                if (o == null)
                    return false
                else if (o is Input) {
                    return o.name == name
                } else if (o is String) {
                    return o == this.name
                }
                return false
            }

            override fun hashCode(): Int {
                return name.hashCode()
            }

        }
    }

}
