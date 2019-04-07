//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.input.lwjgl3

import org.lwjgl.glfw.GLFW.GLFW_PRESS
import org.lwjgl.glfw.GLFW.glfwGetMouseButton

import xyz.chunkstories.api.client.Client
import xyz.chunkstories.api.input.Input
import xyz.chunkstories.api.input.Mouse.MouseButton
import xyz.chunkstories.input.Pollable

class Lwjgl3MouseButton(override val mouse: Lwjgl3Mouse, override val name: String, private val button: Int) : MouseButton, Pollable {

    var isDown = false

    override val isPressed: Boolean
        get() {
            val ingameClient = mouse.im.gameWindow.client.ingame
            return if (ingameClient != null) isDown && ingameClient.player.hasFocus() else isDown
        }

    override val hash: Long
        get() {
            return button.toLong()
        }

    override fun updateStatus() {
        isDown = glfwGetMouseButton(mouse.im.gameWindow.glfwWindowHandle, button) == GLFW_PRESS
    }

    override val client: Client
        get() = mouse.im.gameWindow.client

    override fun equals(o: Any?): Boolean {
        if (o == null)
            return false
        else if (o is Input) {
            return o.name == name
        } else if (o is String) {
            return o == this.name
        }
        return false
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

}
