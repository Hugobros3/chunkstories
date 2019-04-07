package xyz.chunkstories.client.glfw

import xyz.chunkstories.api.graphics.Window
import xyz.chunkstories.client.ClientImplementation
import org.lwjgl.glfw.GLFW.*
import org.slf4j.LoggerFactory
import xyz.chunkstories.client.util.loadIcons
import xyz.chunkstories.graphics.GraphicsBackendsEnum
import xyz.chunkstories.graphics.GraphicsEngineImplementation
import java.awt.image.BufferedImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import javax.imageio.ImageIO
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Unit
import kotlin.UnsupportedOperationException

/** Graphics-graphicsBackend independant implementation of the game window interface.
 * Does provide input management through the use of GLFW's input API
 *
 * For now only implements a Vulkan graphicsBackend
 */
class GLFWWindow(val client: ClientImplementation, val graphicsEngine: GraphicsEngineImplementation, selectedBackend: GraphicsBackendsEnum, title: String) : Window {
    var title = title
        set(value) {
            field = value
            mainThread { glfwSetWindowTitle(glfwWindowHandle, value) }
        }

    override var width: Int = 1024
    override var height: Int = 640

    val glfwWindowHandle: Long
    //val graphicsBackend: GLFWBasedGraphicsBackend
    //val inputsManager : Lwjgl3ClientInputsManager

    init {
        glfwSetErrorCallback { error, description ->
            println("GLFW error: error: $error description: $description")
        }

        val monitors = glfwGetMonitors()!!
        for(i in 0 until monitors.limit()) {
            val monitor = monitors[i]
            val xScale = floatArrayOf(0f)
            val yScale = floatArrayOf(0f)
            glfwGetMonitorContentScale(monitor, xScale, yScale)
            //glfwGetMonitor
            val name = glfwGetMonitorName(monitor)

            val xmm = intArrayOf(0)
            val ymm = intArrayOf(0)

            glfwGetMonitorPhysicalSize(monitor, xmm, ymm)
            println("MONITOR $name scale = [${xScale[0]}:${yScale[0]}] [${xmm[0]}:${ymm[0]}]")
        }

        glfwWindowHint(GLFW_SCALE_TO_MONITOR, GLFW_FALSE)

        glfwWindowHint(GLFW_CLIENT_API, selectedBackend.glfwApiHint)

        glfwWindowHandle = glfwCreateWindow(width, height, title, 0L, 0L)
        if(glfwWindowHandle == 0L)
            throw Exception("Failed to create GLFW window")

        loadIcons()
    }

    fun executeMainThreadChores() {
        mainThreadQueue.removeAll { it.invoke(this); true }
    }

    fun checkStillInFocus() {
        inFocus = glfwGetWindowAttrib(glfwWindowHandle, GLFW_FOCUSED) == org.lwjgl.glfw.GLFW.GLFW_TRUE
    }

    fun cleanup()
    {
        executeMainThreadChores()

        glfwDestroyWindow(glfwWindowHandle)
        glfwTerminate()
    }

    var inFocus = false
    override fun hasFocus(): Boolean = inFocus

    override fun takeScreenshot() {
        try {
            val image: BufferedImage = graphicsEngine.backend.captureFramebuffer()

            val cal = Calendar.getInstance()
            val sdf = SimpleDateFormat("YYYY.MM.dd HH.mm.ss")

            ImageIO.write(image, "PNG", File("./screenshots/${sdf.format(cal.time)}.png"))
        } catch( err : UnsupportedOperationException) {
            client.print("This graphicsBackend doesn't support taking screenshots !")
        }
    }

    private val mainThreadQueue = ConcurrentLinkedDeque<GLFWWindow.() -> Unit>()
    /** Schedules some work to be executed on the main thread (glfw spec requires so) */
    fun mainThread(function: GLFWWindow.() -> Unit) {
        mainThreadQueue.addLast(function)
    }

    val shouldClose
        get() = org.lwjgl.glfw.GLFW.glfwWindowShouldClose(glfwWindowHandle)

    companion object {
        val logger = LoggerFactory.getLogger("client.glfw")
    }
}