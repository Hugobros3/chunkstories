//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.util

/** For loading platform-specific dlls and stuff */
enum class SupportedOS {
    WINDOWS,
    LINUX,
    OSX,
}

object OSHelper {
    val os: SupportedOS

    init {
        val osString = System.getProperty("os.name").toLowerCase()
        os = when {
            osString.contains("windows") -> SupportedOS.WINDOWS
            osString.contains("mac") -> SupportedOS.OSX
            osString.contains("linux") -> SupportedOS.LINUX
            osString.contains("bsd") || osString.contains("unix") || osString.contains("sunos") -> throw Exception("Unsupported os: $osString")
            else -> throw Exception("Unreognised os; $osString")
        }
    }

}
