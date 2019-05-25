//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.input.lwjgl3

import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFW.GLFW_PRESS
import org.lwjgl.glfw.GLFW.glfwGetKey

import xyz.chunkstories.api.input.KeyboardKeyInput
import xyz.chunkstories.input.Pollable

/**
 * Describes a key assignated to some action
 */
class Lwjgl3KeyBind internal constructor(inputsManager: Lwjgl3ClientInputsManager, name: String, defaultKeyName: String, val hidden: Boolean, val repeat: Boolean) : Lwjgl3Input(inputsManager, name), KeyboardKeyInput, Pollable {

    /**
     * Internal to the engine, should not be interfered with by external mods
     *
     * @return
     */
    internal var lwjglKey: Int = 0
        private set
    private val defaultKey: Int = GLFWKeyIndexHelper.getGlfwKeyByName(defaultKeyName) ?: GLFW.GLFW_KEY_UNKNOWN

    private var isDown = false

    init {
        this.lwjglKey = defaultKey

        val client = inputsManager.gameWindow.client
        val clientConfiguration = client.configuration

        if (!hidden) {
            val option = clientConfiguration.OptionKeyBind("client.input.bind.$name", defaultKey)
            option.addHook { lwjglKey = this.value }
            clientConfiguration.registerOption(option)
        }
    }

    /** Returns true if the key is pressed and we're either not ingame or there is no GUI overlay blocking gameplay input  */
    override val isPressed: Boolean
        get(): Boolean {
            val ingameClient = inputsManager.gameWindow.client.ingame
            return if (ingameClient != null) isDown && ingameClient.player.hasFocus() else isDown
        }


    override fun updateStatus() {
        isDown = glfwGetKey(inputsManager.gameWindow.glfwWindowHandle, lwjglKey) == GLFW_PRESS
    }
}
