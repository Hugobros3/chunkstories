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

class Lwjgl3MouseButton(private val mouse: Lwjgl3Mouse, private val name: String, private val button: Int) : MouseButton, Pollable {

    private var isDown = false

    override fun getName(): String {
        return name
    }

    override fun isPressed(): Boolean {
        val ingameClient = mouse.im.gameWindow.client.ingame
        return if (ingameClient != null) isDown && ingameClient.player.hasFocus() else isDown
    }

    override fun getHash(): Long {
        return button.toLong()
    }

    override fun updateStatus() {
        isDown = glfwGetMouseButton(mouse.im.gameWindow.glfwWindowHandle, button) == GLFW_PRESS
    }

    override fun getClient(): Client {
        return mouse.im.gameWindow.client
    }

    override fun getMouse(): Lwjgl3Mouse {
        return mouse
    }

    override fun equals(o: Any?): Boolean {
        if (o == null)
            return false
        else if (o is Input) {
            return o.name == getName()
        } else if (o is String) {
            return o == this.getName()
        }
        return false
    }

    override fun hashCode(): Int {
        return getName().hashCode()
    }

}
