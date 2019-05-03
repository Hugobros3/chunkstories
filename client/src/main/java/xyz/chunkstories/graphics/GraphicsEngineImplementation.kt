package xyz.chunkstories.graphics

import org.lwjgl.glfw.GLFW
import xyz.chunkstories.api.graphics.GraphicsEngine
import xyz.chunkstories.api.graphics.rendergraph.RenderGraphDeclarationScript
import xyz.chunkstories.client.ClientImplementation
import xyz.chunkstories.client.glfw.GLFWWindow
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.util.VersionInfo

class GraphicsEngineImplementation(val client: ClientImplementation) : GraphicsEngine, Cleanable {

    override val backend: GLFWBasedGraphicsBackend

    val window: GLFWWindow

    override val representationsProviders = RepresentationsProvidersImplem()

    override val textures: GraphicsEngine.Textures
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    private fun pickBackend(): GraphicsBackendsEnum {
        val configuredBackend = client.arguments["backend"] ?: client.configuration.getValue("client.graphics.backend")

        // If one backend was configured by the user, try to find it, otherwise use Vulkan by default
        var backendToUse = GraphicsBackendsEnum.values().find { it.name.toLowerCase() == configuredBackend.toLowerCase() } ?: GraphicsBackendsEnum.VULKAN

        // If the selected or default backend isn't usable, use OpenGL as a failsafe
        if(!backendToUse.usable()) {
            //TODO messagebox
            GLFWWindow.logger.warn("$backendToUse can't be used here, defaulting to OpenGL")
            backendToUse = GraphicsBackendsEnum.OPENGL
        }

        return backendToUse
    }

    init {
        if (!GLFW.glfwInit())
            throw Exception("Could not initialize GLFW")

        // Pick a backend !
        val selectedGraphicsBackend = pickBackend()

        // Dirty fix for OSX because le apple is special
        // TODO check for MacOS first.
        // System.setProperty("java.awt.headless", "true")

        window = GLFWWindow(client, this, selectedGraphicsBackend, "Chunk Stories " + VersionInfo.version)

        //TODO modify those to take the GraphicsEngine as param
        backend = selectedGraphicsBackend.init(this, window)
    }

    var frameNumber = 0
    fun renderGame() {
        backend.drawFrame(frameNumber)
        frameNumber++
    }

    override fun loadRenderGraph(declaration: RenderGraphDeclarationScript) {
        backend.queuedRenderGraph = declaration
    }

    override fun cleanup() {
        backend.cleanup()
    }
}