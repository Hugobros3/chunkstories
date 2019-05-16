package xyz.chunkstories.graphics.opengl

import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFW.glfwMakeContextCurrent
import org.lwjgl.glfw.GLFW.glfwSwapBuffers
import org.lwjgl.opengl.ARBClipControl.GL_ZERO_TO_ONE
import org.lwjgl.opengl.ARBClipControl.glClipControl
import org.lwjgl.opengl.ARBDebugOutput.glDebugMessageCallbackARB
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.ARBDirectStateAccess.*
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GLCapabilities
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.graphics.representation.Representation
import xyz.chunkstories.api.graphics.systems.RegisteredGraphicSystem
import xyz.chunkstories.api.graphics.systems.dispatching.ChunksRenderer
import xyz.chunkstories.api.graphics.systems.dispatching.DispatchingSystem
import xyz.chunkstories.api.graphics.systems.drawing.DrawingSystem
import xyz.chunkstories.api.graphics.systems.drawing.FullscreenQuadDrawer
import xyz.chunkstories.api.gui.GuiDrawer
import xyz.chunkstories.client.glfw.GLFWWindow
import xyz.chunkstories.graphics.GLFWBasedGraphicsBackend
import xyz.chunkstories.graphics.GraphicsEngineImplementation
import xyz.chunkstories.graphics.common.WorldRenderer
import xyz.chunkstories.graphics.opengl.graph.OpenglPass
import xyz.chunkstories.graphics.opengl.graph.OpenglRenderGraph
import xyz.chunkstories.graphics.opengl.resources.OpenglSamplers
import xyz.chunkstories.graphics.opengl.shaders.OpenglShaderFactory
import xyz.chunkstories.graphics.opengl.systems.OpenglDispatchingSystem
import xyz.chunkstories.graphics.opengl.systems.OpenglDrawingSystem
import xyz.chunkstories.graphics.opengl.systems.OpenglFullscreenQuadDrawer
import xyz.chunkstories.graphics.opengl.systems.gui.OpenglGuiDrawer
import xyz.chunkstories.graphics.opengl.textures.OpenglTextures
import xyz.chunkstories.graphics.opengl.voxels.OpenglVoxelTexturesArray
import xyz.chunkstories.graphics.opengl.world.OpenglWorldRenderer
import xyz.chunkstories.graphics.opengl.world.chunks.OpenglChunkRepresentationsDispatcher
import xyz.chunkstories.voxel.ReloadableVoxelTextures
import xyz.chunkstories.voxel.VoxelTexturesSupport
import xyz.chunkstories.world.WorldClientCommon
import java.awt.image.BufferedImage
import javax.swing.JOptionPane

class OpenglGraphicsBackend(graphicsEngine: GraphicsEngineImplementation, window: GLFWWindow) : GLFWBasedGraphicsBackend(graphicsEngine, window), VoxelTexturesSupport {
    private val capabilities: GLCapabilities
    private val requiredExtensions = setOf("GL_ARB_debug_output", "GL_ARB_texture_storage", "GL_ARB_direct_state_access", "GL_ARB_draw_buffers_blend")

    var renderGraph: OpenglRenderGraph

    val shaderFactory: OpenglShaderFactory
    val textures: OpenglTextures

    val samplers: OpenglSamplers

    init {
        glfwMakeContextCurrent(window.glfwWindowHandle)
        capabilities = GL.createCapabilities()

        //JOptionPane.showMessageDialog(null, "The OpenGL backend is currently only a placeholder, it does not work yet. Please use the Vulkan backend instead.", "Information", JOptionPane.INFORMATION_MESSAGE)

        checkForExtensions()

        if(debugMode)
            setupDebugMode()

        val vaoDontCare = glCreateVertexArrays()
        glBindVertexArray(vaoDontCare)

        //TODO NO BAD (rekts compatibility)
        glClipControl(GL_LOWER_LEFT, GL_ZERO_TO_ONE)

        shaderFactory = OpenglShaderFactory(this, window.client)
        textures = OpenglTextures(this)
        samplers = OpenglSamplers(this)

        renderGraph = OpenglRenderGraph(this, queuedRenderGraph!!)
        queuedRenderGraph = null

        GLFW.glfwSetFramebufferSizeCallback(window.glfwWindowHandle) { handle, newWidth, newHeight ->
            println("resized to $newWidth:$newHeight")

            if(newWidth != 0 && newHeight != 0) {
                window.width = newWidth
                window.height = newHeight

                GL11.glViewport(0, 0, newWidth, newHeight)
                renderGraph.resizeBuffers()
            }
        }
    }

    private fun checkForExtensions() {
        val versionString = glGetString(GL_VERSION) ?: "0.0"
        println("Version:" + versionString)

        //val extensionsString = glGetString(GL_EXTENSIONS) ?: throw Exception("Couldn't list extensions")
        //val extensionsList = extensionsString.split(" ")
        val extensionsCount = glGetInteger(GL_NUM_EXTENSIONS)
        val extensionsList = Array(extensionsCount) { glGetStringi(GL_EXTENSIONS, it)}.toList()

        println("Extensions: "+extensionsList)

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
        /*if(System.currentTimeMillis() % 2000L < 1000L)
            glClearColor(1.0f, 0f, 0f, 1.0f)
        else
            glClearColor(1.0f, 0.2f, 0f, 1.0f)

        glClear(GL_COLOR_BUFFER_BIT)*/
        val queuedRenderGraph = this.queuedRenderGraph
        if(queuedRenderGraph != null) {
            //TODO do we need this ?
            glFinish()

            renderGraph.cleanup()
            renderGraph = OpenglRenderGraph(this, queuedRenderGraph!!)
            this.queuedRenderGraph = null
        }

        val frame = OpenglFrame(frameNumber, System.currentTimeMillis())
        renderGraph.renderFrame(frame)

        glfwSwapBuffers(window.glfwWindowHandle)
    }

    override fun captureFramebuffer(): BufferedImage {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createWorldRenderer(world: WorldClientCommon): WorldRenderer =
            OpenglWorldRenderer(this, world)

    fun <T : DrawingSystem> createDrawingSystem(pass: OpenglPass, registration: RegisteredGraphicSystem<T>): OpenglDrawingSystem {
        val dslCode = registration.dslCode as DrawingSystem.() -> Unit

        return when(registration.clazz) {
            GuiDrawer::class.java -> OpenglGuiDrawer(pass, dslCode)
            FullscreenQuadDrawer::class.java -> OpenglFullscreenQuadDrawer(pass, dslCode)
            else -> throw Exception("Unimplemented system on this backend: ${registration.clazz}")
        }
    }

    fun <T : DispatchingSystem> getOrCreateDispatchingSystem(list: MutableList<OpenglDispatchingSystem<*>>, dispatchingSystemRegistration: RegisteredGraphicSystem<T>) : OpenglDispatchingSystem<*> {
        val implemClass: Class<out OpenglDispatchingSystem<out Representation>> = when(dispatchingSystemRegistration.clazz) {
            ChunksRenderer::class.java -> OpenglChunkRepresentationsDispatcher::class
            else -> throw Exception("Unimplemented system on this backend: ${dispatchingSystemRegistration.clazz}")
        }.java

        val existing = list.find { implemClass.isAssignableFrom(it::class.java) }
        if(existing != null)
            return existing

        val new: OpenglDispatchingSystem<out Representation> = when(dispatchingSystemRegistration.clazz) {
            ChunksRenderer::class.java -> OpenglChunkRepresentationsDispatcher(this)
            else -> throw Exception("Unimplemented system on this backend: ${dispatchingSystemRegistration.clazz}")
        }

        list.add(new)

        return new
    }

    override fun createVoxelTextures(voxels: Content.Voxels) = OpenglVoxelTexturesArray(this, voxels)

    override fun reloadRendergraph() {
        this.queuedRenderGraph = this.renderGraph.dslCode
    }

    override fun cleanup() {
        renderGraph.cleanup()
        samplers.cleanup()
        logger.debug("OpenGL backend done cleaning !")
    }

    companion object {
        var debugMode = true
        val logger = LoggerFactory.getLogger("client.gfx_gl")
    }
}