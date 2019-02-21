//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.world

class WorldLoadingException : Exception {

    constructor(message: String, cause: Throwable) : super(message, cause) {}

    override val message: String?
        get() {
            return if (cause != null) super.message + ": " + cause.javaClass.simpleName + "\n" + cause.message else super.message
        }

    constructor(message: String) : super(message) {}

    companion object {

        private val serialVersionUID = -7131921980416653390L
    }

}
