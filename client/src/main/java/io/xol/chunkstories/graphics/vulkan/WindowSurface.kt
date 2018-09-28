package io.xol.chunkstories.graphics.vulkan

import io.xol.chunkstories.client.glfw.GLFWWindow
import org.lwjgl.glfw.GLFWVulkan.*
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR
import org.lwjgl.vulkan.VkInstance

class WindowSurface(private val vkInstance: VkInstance, glfwWindow: GLFWWindow) {
    val handle: VkSurfaceKHR

    init {
        stackPush()
        val pSurface = stackMallocLong(1)
        //lol why would you bother with this
        glfwCreateWindowSurface(vkInstance, glfwWindow.glfwWindowHandle, null, pSurface)
        handle = pSurface.get(0)
        stackPop()
    }

    fun cleanup() {
        vkDestroySurfaceKHR(vkInstance, handle, null)
    }
}