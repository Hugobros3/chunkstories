//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.input

import xyz.chunkstories.api.input.Input

/**
 * An input not linked to actual hardware directly, either representing a remote
 * input or an input used for internal purposes ( like actions buttons,
 * 'pressed' by the client to tell the master what they did with fancy
 * semantics, see shootGun in res/virtual.inputs
 */
class InputVirtual(inputsManager: CommonInputsManager, name: String) : AbstractInput(inputsManager, name) {
    override var isPressed = false

    override fun toString(): String {
        return "[KeyBindVirtual: $name]"
    }

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
