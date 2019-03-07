//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.input.lwjgl3

import xyz.chunkstories.api.input.Input
import xyz.chunkstories.input.InputsLoaderHelper

abstract class Lwjgl3Input(protected val im: Lwjgl3ClientInputsManager, override val name: String) : Input {

    override var hash: Long = 0

    init {
        computeHash(name)
    }

    private fun computeHash(name2: String) {
        val digested = InputsLoaderHelper.md.digest(name2.toByteArray())
        hash = hash and 0x0FFFFFFFFFFFFFFFL or (digested[0].toLong() and 0xF shl 60)
        hash = hash and -0xf00000000000001L or (digested[1].toLong() and 0xF shl 56)
        hash = hash and -0xf0000000000001L or (digested[2].toLong() and 0xF shl 52)
        hash = hash and -0xf000000000001L or (digested[3].toLong() and 0xF shl 48)
        hash = hash and -0xf00000000001L or (digested[4].toLong() and 0xF shl 44)
        hash = hash and -0xf0000000001L or (digested[5].toLong() and 0xF shl 40)
        hash = hash and -0xf000000001L or (digested[6].toLong() and 0xF shl 36)
        hash = hash and -0xf00000001L or (digested[7].toLong() and 0xF shl 32)
        hash = hash and -0xf0000001L or (digested[8].toLong() and 0xF shl 28)
        hash = hash and -0xf000001L or (digested[9].toLong() and 0xF shl 24)
        hash = hash and -0xf00001L or (digested[10].toLong() and 0xF shl 20)
        hash = hash and -0xf0001L or (digested[11].toLong() and 0xF shl 16)
        hash = hash and -0xf001L or (digested[12].toLong() and 0xF shl 12)
        hash = hash and -0xf01L or (digested[13].toLong() and 0xF shl 8)
        hash = hash and -0xf1L or (digested[14].toLong() and 0xF shl 4)
        hash = hash and -0x10L or (digested[15].toLong() and 0xF shl 0)
    }

    override fun equals(o: Any?): Boolean {
        if (o == null)
            return false
        else if (o is Lwjgl3Input) {
            return o.name == name
        } else if (o is String) {
            return o == this.name
        }
        return false
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    abstract fun reload()
}
