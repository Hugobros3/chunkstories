//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.input.lwjgl3

import org.lwjgl.glfw.GLFW

object GLFWKeyIndexHelper {
    val glfwKeyCodes: Map<String, Int> by lazy {
        val codes = mutableMapOf<String, Int>()

        val fields = GLFW::class.java.fields
        for (f in fields) {
            if (f.name.startsWith("GLFW_KEY_")) {
                val value = f.get(null)
                val iValue = value as Int

                val fullName = f.name
                val shortName = fullName.substring(9)

                codes[fullName] = iValue
                codes[shortName] = iValue
            }
        }

        codes
    }

    val shortNames: Map<Int, String> by lazy {
        val codes = mutableMapOf<Int, String>()

        val fields = GLFW::class.java.fields
        for (f in fields) {
            if (f.name.startsWith("GLFW_KEY_")) {
                val value = f.get(null)
                val iValue = value as Int

                val fullName = f.name
                val shortName = fullName.substring(9)

                codes[iValue] = shortName
            }
        }

        codes
    }

    fun getGlfwKeyByName(keyName: String): Int? {
        val i: Int? = glfwKeyCodes[keyName]
        return i
    }
}
