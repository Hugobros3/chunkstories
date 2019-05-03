package xyz.chunkstories.graphics.opengl

import org.lwjgl.glfw.GLFW.glfwMakeContextCurrent
import org.lwjgl.glfw.GLFW.glfwSwapBuffers
import org.lwjgl.opengl.ARBDebugOutput.glDebugMessageCallbackARB
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GLCapabilities
import org.slf4j.LoggerFactory
import xyz.chunkstories.client.glfw.GLFWWindow
import xyz.chunkstories.graphics.GLFWBasedGraphicsBackend
import xyz.chunkstories.graphics.GraphicsEngineImplementation
import xyz.chunkstories.graphics.common.WorldRenderer
import xyz.chunkstories.world.WorldClientCommon
import java.awt.image.BufferedImage
import javax.swing.JOptionPane

class OpenglGraphicsBackend(graphicsEngine: GraphicsEngineImplementation, window: GLFWWindow) : GLFWBasedGraphicsBackend(graphicsEngine, window) {
    private val capabilities: GLCapabilities

    private val requiredExtensions = setOf("GL_ARB_debug_output", "GL_ARB_direct_state_access")

    init {
        glfwMakeContextCurrent(window.glfwWindowHandle)
        capabilities = GL.createCapabilities()

        JOptionPane.showMessageDialog(null, "The OpenGL backend is currently only a placeholder, it does not work yet. Please use the Vulkan backend instead.", "Information", JOptionPane.INFORMATION_MESSAGE)

        checkForExtensions()

        if(debugMode)
            setupDebugMode()
    }

    private fun checkForExtensions() {
        val extensionsString = glGetString(GL_EXTENSIONS) ?: ""
        val extensionsList = extensionsString.split(" ")
        //println(extensionsList)

        if(!extensionsList.containsAll(requiredExtensions)) {
            JOptionPane.showMessageDialog(null, """
                Your video card does not support the required OpenGL extensions ($requiredExtensions)

                To play this game you may need to upgrade your card, or select the dedicated video card to run this game if your laptop has switchable graphics.
                You can also try to update your drivers, or use different drivers if you are on linux.
                """.trimIndent(), "Unsupported configuration", JOptionPane.ERROR_MESSAGE)
            System.exit(0)
        }
    }

    private fun setupDebugMode() {
        logger.debug("Enabling debug mode: setting callback.")
        glDebugMessageCallbackARB(OpenGLDebugOutputCallback(), 0)
    }

    override fun drawFrame(frameNumber: Int) {
        if(System.currentTimeMillis() % 2000L < 1000L)
            glClearColor(1.0f, 0f, 0f, 1.0f)
        else
            glClearColor(1.0f, 0.2f, 0f, 1.0f)

        glClear(GL_COLOR_BUFFER_BIT)

        glfwSwapBuffers(window.glfwWindowHandle)
    }

    override fun captureFramebuffer(): BufferedImage {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createWorldRenderer(world: WorldClientCommon): WorldRenderer {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun cleanup() {

    }

    companion object {
        var debugMode = true
        val logger = LoggerFactory.getLogger("client.gfx_gl")
    }
}