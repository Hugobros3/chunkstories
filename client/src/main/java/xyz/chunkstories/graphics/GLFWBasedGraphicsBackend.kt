package xyz.chunkstories.graphics

import xyz.chunkstories.api.graphics.GraphicsBackend
import xyz.chunkstories.api.graphics.rendergraph.RenderGraphDeclarationScript
import xyz.chunkstories.client.glfw.GLFWWindow
import xyz.chunkstories.graphics.vulkan.util.BuiltInRendergraphs
import java.awt.image.BufferedImage

/** This implementaiton uses GLFW and so the backends have to hook into that ! */
abstract class GLFWBasedGraphicsBackend(val window: GLFWWindow) : GraphicsBackend {
    var queuedRenderGraph: RenderGraphDeclarationScript? = BuiltInRendergraphs.onlyGuiRenderGraph

    abstract fun drawFrame(frameNumber : Int)

    abstract fun cleanup()
    abstract fun captureFramebuffer(): BufferedImage
}