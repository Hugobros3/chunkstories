package xyz.chunkstories.graphics

import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWVulkan
import xyz.chunkstories.client.glfw.GLFWWindow
import xyz.chunkstories.graphics.opengl.OpenglGraphicsBackend
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend

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

        if(OpenglGraphicsBackend.debugMode)
            glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE)
    }, {
        true
    }, { e, w ->
        OpenglGraphicsBackend(e, w)
    });
}