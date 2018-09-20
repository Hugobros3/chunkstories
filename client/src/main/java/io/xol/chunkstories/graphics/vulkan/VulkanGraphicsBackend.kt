package io.xol.chunkstories.graphics.vulkan

import io.xol.chunkstories.api.graphics.GraphicsBackend
import io.xol.chunkstories.api.graphics.systems.drawing.DrawingSystem
import io.xol.chunkstories.api.graphics.systems.drawing.FarTerrainDrawer
import io.xol.chunkstories.client.glfw.GLFWWindow

class VulkanGraphicsBackend(val window: GLFWWindow) : GraphicsBackend {

    override fun createDrawingSystem(clazz: Class<DrawingSystem>): DrawingSystem {
        when(clazz) {
            FarTerrainDrawer::class.java -> {
                TODO("you have to implement the common drawing systems in your backend")
            }
        }

        TODO("not implemented")
    }

}