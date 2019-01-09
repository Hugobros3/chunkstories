package xyz.chunkstories.graphics

import xyz.chunkstories.api.dsl.RenderGraphDeclarationScript
import xyz.chunkstories.api.graphics.GraphicsBackend
import xyz.chunkstories.client.glfw.GLFWWindow
import xyz.chunkstories.graphics.vulkan.util.BuiltInRendergraphs
import java.awt.image.BufferedImage
import java.lang.Exception

/** This implementaiton uses GLFW and so the backends have to hook into that ! */
abstract class GLFWBasedGraphicsBackend(val window: GLFWWindow) : GraphicsBackend {
    var queuedRenderGraph: RenderGraphDeclarationScript? = BuiltInRendergraphs.onlyGuiRenderGraph

    abstract fun drawFrame(frameNumber : Int)

    abstract fun cleanup()
    abstract fun captureFramebuffer(): BufferedImage
}