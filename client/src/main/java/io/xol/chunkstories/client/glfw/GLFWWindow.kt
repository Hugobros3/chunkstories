package io.xol.chunkstories.client.glfw

import io.xol.chunkstories.api.graphics.GraphicsBackend
import io.xol.chunkstories.api.graphics.Window
import io.xol.chunkstories.client.ClientImplementation
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.input.lwjgl3.Lwjgl3ClientInputsManager
import org.lwjgl.glfw.GLFW.*
import java.lang.Exception

/** Graphics-backend independant implementation of the game window interface.
 * Does provide input management through the use of GLFW's input API
 *
 * For now only implements a Vulkan backend
 */
class GLFWWindow(val client: ClientImplementation) : Window {
    constructor(client: ClientImplementation, title: String) : this(client) {
        this.title = title
    }

    var title = "Untitled"
        set(value) {
            field = value
            glfwSetWindowTitle(glfwWindowHandle, value)
        }

    override var height: Int = 1024
    override var width: Int = 640

    val glfwWindowHandle: Long
    val backend: GraphicsBackend

    val inputsManager : Lwjgl3ClientInputsManager

    init {
        if (!glfwInit())
            throw Exception("Could not initialize GLFW")

        backend = VulkanGraphicsBackend(this)

        glfwWindowHandle = glfwCreateWindow(width, height, title, 0L, 0L)
        if(glfwWindowHandle == 0L)
            throw Exception("Failed to create GLFW window")

        inputsManager = Lwjgl3ClientInputsManager(this)
    }

    fun mainLoop() {

    }

    override fun hasFocus(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun takeScreenshot(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}