package xyz.chunkstories.graphics

import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL11.*
import org.lwjgl.glfw.GLFWVulkan
import xyz.chunkstories.client.glfw.GLFWWindow
import xyz.chunkstories.graphics.opengl.OpenglGraphicsBackend
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.util.OSHelper
import xyz.chunkstories.util.SupportedOS

enum class GraphicsBackendsEnum(val glfwInitHints: () -> Unit, val usable: () -> Boolean, val init: (GraphicsEngineImplementation, GLFWWindow) -> GLFWBasedGraphicsBackend) {
    VULKAN({
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API)
    }, {
        GLFWVulkan.glfwVulkanSupported()
    }, { e, w ->
        VulkanGraphicsBackend(e, w)
    }),

    OPENGL({
        glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API)

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
        
        if(OSHelper.os == SupportedOS.OSX)
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE)

        if(OpenglGraphicsBackend.debugMode)
            glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE)
    }, {
        true
    }, { e, w ->
        OpenglGraphicsBackend(e, w)
    });
}