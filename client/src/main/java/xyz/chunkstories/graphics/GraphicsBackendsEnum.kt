package xyz.chunkstories.graphics

import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWVulkan
import xyz.chunkstories.client.glfw.GLFWWindow
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend

enum class GraphicsBackendsEnum(val glfwApiHint: Int, val usable: () -> Boolean, val init : (GraphicsEngineImplementation, GLFWWindow) -> GLFWBasedGraphicsBackend) {
    VULKAN(GLFW.GLFW_NO_API, { GLFWVulkan.glfwVulkanSupported() },  { e, w -> VulkanGraphicsBackend(e, w) }),
    OPENGL(GLFW.GLFW_OPENGL_API, { true }, { e, w -> TODO("Not implemented yet !") });
}