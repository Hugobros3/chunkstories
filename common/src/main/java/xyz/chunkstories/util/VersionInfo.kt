//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.util

import com.google.gson.Gson
import java.io.File

object VersionInfo {
    val versionJson: VersionJson
    val networkProtocolVersion = 37

    init {
        val file = File("version.json")

        versionJson = if (file.exists()) {
            val gson = Gson()
            gson.fromJson(file.readText(), VersionJson::class.java)!!
        } else {
            VersionJson("unknown", "unknown(no version.json)", "unknown", "unknown")
        }
    }
}

data class VersionJson(val version: String, val verboseVersion: String, val commit: String, val buildtime: String)