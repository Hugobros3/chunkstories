package xyz.chunkstories.graphics

import xyz.chunkstories.api.graphics.GraphicsBackend
import xyz.chunkstories.api.graphics.rendergraph.RenderGraphDeclaration
import xyz.chunkstories.client.glfw.GLFWWindow
import xyz.chunkstories.graphics.common.CommonGraphicsOptions
import xyz.chunkstories.graphics.common.WorldRenderer
import xyz.chunkstories.graphics.vulkan.util.BuiltInRendergraphs
import xyz.chunkstories.world.WorldClientCommon
import java.awt.image.BufferedImage

/** This implementaiton uses GLFW and so the backends have to hook into that ! */
abstract class GLFWBasedGraphicsBackend(val graphicsEngine: GraphicsEngineImplementation, val window: GLFWWindow) : GraphicsBackend {
    var queuedRenderGraph: (RenderGraphDeclaration.() -> Unit)? = BuiltInRendergraphs.onlyGuiRenderGraph(window.client)

    init {
        window.client.configuration.addOptions(CommonGraphicsOptions.create(this))
    }

    abstract fun drawFrame(frameNumber : Int)

    abstract fun cleanup()
    abstract fun captureFramebuffer(): BufferedImage

    abstract fun createWorldRenderer(world: WorldClientCommon): WorldRenderer
    abstract fun reloadRendergraph()
}