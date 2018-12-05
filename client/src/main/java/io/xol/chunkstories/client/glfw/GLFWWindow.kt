package io.xol.chunkstories.client.glfw

import io.xol.chunkstories.api.entity.traits.serializable.TraitControllable
import io.xol.chunkstories.api.graphics.Window
import io.xol.chunkstories.client.ClientImplementation
import io.xol.chunkstories.graphics.GLFWBasedGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.input.lwjgl3.Lwjgl3ClientInputsManager
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported
import org.slf4j.LoggerFactory
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
class GLFWWindow(val client: ClientImplementation) : Window {
    constructor(client: ClientImplementation, title: String) : this(client) {
        this.title = title
    }

    var title = "Untitled"
        set(value) {
            field = value
            mainThread { glfwSetWindowTitle(glfwWindowHandle, value) }
        }

    override var width: Int = 1024
    override var height: Int = 640

    val glfwWindowHandle: Long
    val graphicsBackend: GLFWBasedGraphicsBackend
    val inputsManager : Lwjgl3ClientInputsManager

    enum class GraphicsBackends(val glfwApiHint: Int, val usable: () -> Boolean, val creator : (GLFWWindow) -> GLFWBasedGraphicsBackend) {
        VULKAN(GLFW_NO_API, { glfwVulkanSupported() },  { VulkanGraphicsBackend(it) }),
        OPENGL(GLFW_OPENGL_API, { true }, { TODO("Not implemented yet !") });
    }

    private fun pickBackend(): GraphicsBackends {
        val configuredBackend = client.configuration.getValue("client.graphics.backend")
        // If one backend was configured by the user, try to find it, otherwise use Vulkan by default
        var backendToUse = GraphicsBackends.values().find { it.name == configuredBackend } ?: GraphicsBackends.VULKAN

        // If the selected or default backend isn't usable, use OpenGL as a failsafe
        if(!backendToUse.usable()) {
            //TODO messagebox
            logger.warn("$backendToUse can't be used here, defaulting to OpenGL")
            backendToUse = GraphicsBackends.OPENGL
        }

        return backendToUse
    }

    init {
        if (!glfwInit())
            throw Exception("Could not initialize GLFW")

        // Pick a backend !
        val selectedGraphicsBackend = pickBackend()

        glfwWindowHint(GLFW_CLIENT_API, selectedGraphicsBackend.glfwApiHint)
        glfwWindowHandle = glfwCreateWindow(width, height, title, 0L, 0L)
        if(glfwWindowHandle == 0L)
            throw Exception("Failed to create GLFW window")

        graphicsBackend = selectedGraphicsBackend.creator(this)
        inputsManager = Lwjgl3ClientInputsManager(this)
    }

    var frameNumber = 0

    fun mainLoop() {
        while(!glfwWindowShouldClose(glfwWindowHandle)) {
            mainThreadQueue.removeAll { it.invoke(this); true }
            glfwPollEvents()
            inputsManager.updateInputs()
            client.soundManager.updateAllSoundSources()

            client.ingame?.player?.controlledEntity?.let { it.traits[TraitControllable::class]?.onEachFrame() }

            graphicsBackend.drawFrame(frameNumber)

            inFocus = glfwGetWindowAttrib(glfwWindowHandle, GLFW_FOCUSED) == GLFW_TRUE
            frameNumber++
        }

        client.ingame?.exitToMainMenu()

        cleanup()
    }

    fun cleanup() {
        graphicsBackend.cleanup()
        glfwDestroyWindow(glfwWindowHandle)
        glfwTerminate()
    }

    var inFocus = false
    override fun hasFocus(): Boolean = inFocus

    override fun takeScreenshot() {
        try {
            val image: BufferedImage = graphicsBackend.captureFramebuffer()

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

    companion object {
        val logger = LoggerFactory.getLogger("client.glfw")
    }
}