//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.input.lwjgl3

import org.lwjgl.glfw.GLFW
import xyz.chunkstories.api.input.KeyboardKeyInput

class Lwjgl3KeyBindCompound(im: Lwjgl3ClientInputsManager, name: String, internal val defaultKeysNames: String) : Lwjgl3Input(im, name), KeyboardKeyInput {
    internal lateinit var glfwKeys: IntArray

    init {
        reload()
    }

    override val isPressed: Boolean
        get() {
            return false
        }

    override fun reload() {
        val keyNamesString = defaultKeysNames
        val keyNames = keyNamesString.split("\\+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        glfwKeys = IntArray(keyNames.size)
        for (i in keyNames.indices) {
            val keyName = keyNames[i]

            val glfwKey = GLFWKeyIndexHelper.getGlfwKeyByName(keyName)
            glfwKeys[i] = glfwKey ?: GLFW.GLFW_KEY_UNKNOWN
            // System.out.println(keyName+":"+glfwKey);
        }

        this.im.logger().debug("Initialized keyBindCompound " + name + " for " + glfwKeys.size + " keys.")
    }

}
