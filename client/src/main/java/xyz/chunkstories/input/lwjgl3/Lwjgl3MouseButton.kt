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
import xyz.chunkstories.input.AbstractInput
import xyz.chunkstories.input.Pollable

class Lwjgl3MouseButton(override val mouse: Lwjgl3Mouse, name: String, private val button: Int) : AbstractInput(mouse.inputsManager, name), MouseButton, Pollable {

    var isDown = false

    override val isPressed: Boolean
        get() {
            val ingameClient = mouse.inputsManager.gameWindow.client.ingame
            return if (ingameClient != null) isDown && ingameClient.player.hasFocus() else isDown
        }

    override fun updateStatus() {
        isDown = glfwGetMouseButton(mouse.inputsManager.gameWindow.glfwWindowHandle, button) == GLFW_PRESS
    }

    override val client: Client
        get() = mouse.inputsManager.gameWindow.client

    override fun equals(o: Any?) = when (o) {
        null -> false
        is Input -> o.name == name
        is String -> o == this.name
        else -> false
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

}
