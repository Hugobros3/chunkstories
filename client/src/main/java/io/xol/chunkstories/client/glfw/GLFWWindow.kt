package io.xol.chunkstories.client.glfw

import io.xol.chunkstories.api.graphics.Window
import io.xol.chunkstories.client.ClientImplementation
import io.xol.chunkstories.graphics.GLFWBasedGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.input.lwjgl3.Lwjgl3ClientInputsManager
import org.lwjgl.glfw.GLFW.*
import java.awt.image.BufferedImage
import java.io.File
import java.lang.Exception
import javax.imageio.ImageIO
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque

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
            mainThread { glfwSetWindowTitle(glfwWindowHandle, value) }
        }

    override var height: Int = 1024
    override var width: Int = 640

    val glfwWindowHandle: Long
    val backend: GLFWBasedGraphicsBackend

    val inputsManager : Lwjgl3ClientInputsManager

    init {
        if (!glfwInit())
            throw Exception("Could not initialize GLFW")

        //TODO check if we're using Vulkan Backend before doing this
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API)

        glfwWindowHandle = glfwCreateWindow(width, height, title, 0L, 0L)
        if(glfwWindowHandle == 0L)
            throw Exception("Failed to create GLFW window")

        backend = VulkanGraphicsBackend(this)

        inputsManager = Lwjgl3ClientInputsManager(this)
    }

    var frameNumber = 0

    fun mainLoop() {
        while(!glfwWindowShouldClose(glfwWindowHandle)) {
            mainThreadQueue.removeAll { it.invoke(this); true }
            glfwPollEvents()

            backend.drawFrame(frameNumber)

            inFocus = glfwGetWindowAttrib(glfwWindowHandle, GLFW_FOCUSED) == GLFW_TRUE
            frameNumber++
        }

        cleanup()
    }

    fun cleanup() {
        backend.cleanup()
        glfwDestroyWindow(glfwWindowHandle)
    }

    var inFocus = false
    override fun hasFocus(): Boolean = inFocus

    override fun takeScreenshot() {
        try {
            val image: BufferedImage = backend.captureFramebuffer()

            val cal = Calendar.getInstance()
            val sdf = SimpleDateFormat("YYYY.MM.dd HH.mm.ss")

            ImageIO.write(image, "PNG", File("./screenshots/${sdf.format(cal.time)}.png"))
        } catch( err : UnsupportedOperationException) {
            client.print("This backend doesn't support taking screenshots !")
        }
    }

    val mainThreadQueue = ConcurrentLinkedDeque<GLFWWindow.() -> Unit>()
    /** Schedules some work to be executed on the main thread (glfw spec requires so) */
    fun mainThread(function: GLFWWindow.() -> Unit) {
        mainThreadQueue.addLast(function)
    }
}