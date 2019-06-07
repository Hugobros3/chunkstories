package xyz.chunkstories.graphics.opengl

import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFW.glfwMakeContextCurrent
import org.lwjgl.glfw.GLFW.glfwSwapBuffers
import org.lwjgl.opengl.ARBDebugOutput.glDebugMessageCallbackARB
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.GLCapabilities
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.graphics.representation.Representation
import xyz.chunkstories.api.graphics.systems.RegisteredGraphicSystem
import xyz.chunkstories.api.graphics.systems.dispatching.ChunksRenderer
import xyz.chunkstories.api.graphics.systems.dispatching.DispatchingSystem
import xyz.chunkstories.api.graphics.systems.dispatching.ModelsRenderer
import xyz.chunkstories.api.graphics.systems.drawing.DrawingSystem
import xyz.chunkstories.api.graphics.systems.drawing.FullscreenQuadDrawer
import xyz.chunkstories.api.gui.GuiDrawer
import xyz.chunkstories.client.glfw.GLFWWindow
import xyz.chunkstories.graphics.GLFWBasedGraphicsBackend
import xyz.chunkstories.graphics.GraphicsEngineImplementation
import xyz.chunkstories.graphics.common.WorldRenderer
import xyz.chunkstories.graphics.common.util.getAnimationTime
import xyz.chunkstories.graphics.opengl.graph.OpenglPass
import xyz.chunkstories.graphics.opengl.graph.OpenglRenderGraph
import xyz.chunkstories.graphics.opengl.resources.OpenglSamplers
import xyz.chunkstories.graphics.opengl.shaders.OpenglShaderFactory
import xyz.chunkstories.graphics.opengl.systems.OpenglDispatchingSystem
import xyz.chunkstories.graphics.opengl.systems.OpenglDrawingSystem
import xyz.chunkstories.graphics.opengl.systems.OpenglFullscreenQuadDrawer
import xyz.chunkstories.graphics.opengl.systems.gui.OpenglGuiDrawer
import xyz.chunkstories.graphics.opengl.systems.world.OpenglModelsDispatcher
import xyz.chunkstories.graphics.opengl.textures.OpenglTextures
import xyz.chunkstories.graphics.opengl.voxels.OpenglVoxelTexturesArray
import xyz.chunkstories.graphics.opengl.world.OpenglWorldRenderer
import xyz.chunkstories.graphics.opengl.world.chunks.OpenglChunkRepresentationsDispatcher
import xyz.chunkstories.graphics.vulkan.swapchain.PerformanceCounter
import xyz.chunkstories.voxel.VoxelTexturesSupport
import xyz.chunkstories.world.WorldClientCommon
import java.awt.image.BufferedImage
import javax.swing.JOptionPane

data class OpenglSupport(val dsaSupport: Boolean)

class OpenglGraphicsBackend(graphicsEngine: GraphicsEngineImplementation, window: GLFWWindow) : GLFWBasedGraphicsBackend(graphicsEngine, window), VoxelTexturesSupport {
    private val capabilities: GLCapabilities
    private val requiredExtensions = setOf("GL_ARB_debug_output", "GL_ARB_texture_storage", "GL_ARB_draw_buffers_blend")
    val openglSupport: OpenglSupport

    var renderGraph: OpenglRenderGraph

    val shaderFactory: OpenglShaderFactory
    val textures: OpenglTextures

    val samplers: OpenglSamplers
    val performance = PerformanceCounter()

    init {
        glfwMakeContextCurrent(window.glfwWindowHandle)
        capabilities = GL.createCapabilities()

        openglSupport = evaluateOpenglSupport()

        if(debugMode)
            setupDebugMode()

        val vaoDontCare = glGenVertexArrays()
        glBindVertexArray(vaoDontCare)

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

                glViewport(0, 0, newWidth, newHeight)
                renderGraph.resizeBuffers()
            }
        }
    }

    private fun evaluateOpenglSupport(): OpenglSupport {
        val versionString = glGetString(GL_VERSION) ?: throw Exception("OpenGL implementation didn't return a GL_VERSION")
        logger.debug("OpenGL Version: $versionString")

        //val extensionsString = glGetString(GL_EXTENSIONS) ?: throw Exception("Couldn't list extensions")
        //val extensionsList = extensionsString.split(" ")
        val extensionsCount = glGetInteger(GL_NUM_EXTENSIONS)
        val extensionsList = Array(extensionsCount) { glGetStringi(GL_EXTENSIONS, it)}.toList()
        logger.debug("OpenGL Extensions: $extensionsList")

        val renderer = glGetString(GL_RENDERER) ?: throw Exception("Can't identify device name (GL_RENDERER returned null)")
        logger.debug("OpenGL Renderer: $renderer")

        if(!extensionsList.containsAll(requiredExtensions)) {
            JOptionPane.showMessageDialog(null, """
                Your video card does not support the required OpenGL extensions ($requiredExtensions)

                To play this game you may need to upgrade your card, or select the dedicated video card to run this game if your laptop has switchable graphics.
                You can also try to update your drivers, or use different drivers if you are on linux.
                """.trimIndent(), "Unsupported configuration", JOptionPane.ERROR_MESSAGE)
            System.exit(0)
        }

        val supportsDsaAtAll = extensionsList.contains("GL_ARB_direct_state_access")

        // AMD didn't fix their DSA until post-2015 drivers, at which point Terascale stuff was EOL
        val devicesWithDodgyDsaDriverSupport = listOf("AMD Radeon HD 6", "AMD Radeon HD 5", "ATI Radeo")
        val dsaBorked = devicesWithDodgyDsaDriverSupport.any { renderer.startsWith(it) }

        val dsaSupport = supportsDsaAtAll && !dsaBorked

        logger.debug("DSA support: $dsaSupport")

        return OpenglSupport(dsaSupport)
    }

    private fun setupDebugMode() {
        logger.debug("Enabling debug mode: setting callback.")
        glDebugMessageCallbackARB(OpenGLDebugOutputCallback(), 0)
    }

    override fun drawFrame(frameNumber: Int) {
        performance.whenFrameBegins()

        val queuedRenderGraph = this.queuedRenderGraph
        if(queuedRenderGraph != null) {
            //TODO do we need this ?
            glFinish()

            renderGraph.cleanup()
            renderGraph = OpenglRenderGraph(this, queuedRenderGraph!!)
            this.queuedRenderGraph = null
        }

        val frame = OpenglFrame(frameNumber, getAnimationTime().toFloat(), System.currentTimeMillis())
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
            ModelsRenderer::class.java -> OpenglModelsDispatcher::class
            ChunksRenderer::class.java -> OpenglChunkRepresentationsDispatcher::class
            else -> throw Exception("Unimplemented system on this backend: ${dispatchingSystemRegistration.clazz}")
        }.java

        val existing = list.find { implemClass.isAssignableFrom(it::class.java) }
        if(existing != null)
            return existing

        val new: OpenglDispatchingSystem<out Representation> = when(dispatchingSystemRegistration.clazz) {
            ModelsRenderer::class.java -> OpenglModelsDispatcher(this)
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