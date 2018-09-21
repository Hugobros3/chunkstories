package io.xol.chunkstories.graphics

import io.xol.chunkstories.api.graphics.GraphicsBackend
import io.xol.chunkstories.client.glfw.GLFWWindow
import java.awt.image.BufferedImage
import java.lang.Exception

/** This implementaiton uses GLFW and so the backends have to hook into that ! */
abstract class GLFWBasedGraphicsBackend(val window: GLFWWindow) : GraphicsBackend {
    abstract fun drawFrame(frameNumber : Int)

    abstract fun cleanup()
    abstract fun captureFramebuffer(): BufferedImage
}