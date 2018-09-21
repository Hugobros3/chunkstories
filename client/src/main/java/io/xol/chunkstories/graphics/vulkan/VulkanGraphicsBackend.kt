package io.xol.chunkstories.graphics.vulkan

import io.xol.chunkstories.api.graphics.systems.drawing.DrawingSystem
import io.xol.chunkstories.api.graphics.systems.drawing.FarTerrainDrawer
import io.xol.chunkstories.client.glfw.GLFWWindow
import io.xol.chunkstories.graphics.GLFWBasedGraphicsBackend
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWVulkan
import org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions
import org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported
import org.lwjgl.vulkan.VkInstance

import java.awt.image.BufferedImage

class VulkanGraphicsBackend(window: GLFWWindow) : GLFWBasedGraphicsBackend(window) {
    private var instance: VkInstance

    init {
        if(!glfwVulkanSupported())
            throw Exception("Vulkan is not supported on this machine")

        val requiredExtensions = glfwGetRequiredInstanceExtensions() ?: throw Exception("Vulkan is not supported for windowed rendering on this machine.")
        instance = createVkInstance(requiredExtensions, true)
    }

    override fun drawFrame(frameNumber: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun cleanup() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun captureFramebuffer(): BufferedImage {
        throw UnsupportedOperationException("Not yet.")
    }

    override fun createDrawingSystem(clazz: Class<DrawingSystem>): DrawingSystem {
        when(clazz) {
            FarTerrainDrawer::class.java -> {
                TODO("you have to implement the common drawing systems in your backend")
            }
        }

        TODO("not implemented")
    }
}