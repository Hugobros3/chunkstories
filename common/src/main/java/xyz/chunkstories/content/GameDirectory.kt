//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.content

import java.io.File

object GameDirectory {
    var chunkStoriesFolder: File? = File(".")

    val gameFolder: String
        get() = if (chunkStoriesFolder == null) "." else chunkStoriesFolder!!.absolutePath

    fun initClientPath() {
        var appDataFolder = System.getProperty("user.dir")
        if (System.getProperty("os.name").toLowerCase().startsWith("win"))
            appDataFolder = System.getenv("APPDATA")
        else if (System.getProperty("os.name").toLowerCase().startsWith("lin"))
            appDataFolder = System.getProperty("user.home")
        else if (System.getProperty("os.name").toLowerCase().startsWith("mac"))
            appDataFolder = System.getProperty("user.home")
        chunkStoriesFolder = File("$appDataFolder/.chunkstories")

        check()
    }

    private fun check() {
        if (!chunkStoriesFolder!!.exists()) {
            val success = chunkStoriesFolder!!.mkdir()
            if (success)
                println("Successfully created .chunkstories folder.")
            else {
                println("Couldn't access or create .chunkstories folder. Exiting.")
                Runtime.getRuntime().exit(0)
            }
        }
        println("Using " + chunkStoriesFolder!!.absolutePath + " as game folder.")
    }
}
