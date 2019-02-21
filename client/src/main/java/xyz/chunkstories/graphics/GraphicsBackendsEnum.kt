package xyz.chunkstories.graphics

import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWVulkan
import xyz.chunkstories.client.glfw.GLFWWindow
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend

enum class GraphicsBackendsEnum(val glfwApiHint: Int, val usable: () -> Boolean, val init : (GLFWWindow) -> GLFWBasedGraphicsBackend) {
    VULKAN(GLFW.GLFW_NO_API, { GLFWVulkan.glfwVulkanSupported() },  { VulkanGraphicsBackend(it) }),
    OPENGL(GLFW.GLFW_OPENGL_API, { true }, { TODO("Not implemented yet !") });
}