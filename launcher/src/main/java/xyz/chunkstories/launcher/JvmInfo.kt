package xyz.chunkstories.launcher

import java.io.File

data class JvmInfo(val jvmType: String, val versionMajor: Int, val versionMinor: Int, val revision: Int, val is64bit: Boolean)

fun checkVersion(): JvmInfo {
    val process = Runtime.getRuntime().exec("java -version", null, File("."))

    val code = process.waitFor()
    if (code != 0) {
        fail("Non-zero error code for 'java -version' ! ($code)")
    }

    val errorOutput = process.errorStream.bufferedReader().readText()

    val firstLine = errorOutput.lines()[0]
    val jvmType = firstLine.split(" ")[0]

    val versionDigits = try {
        var jvmVersion = firstLine.split(" ")[2].removeSurrounding("\"")
        jvmVersion = jvmVersion.substring(0, jvmVersion.indexOf('_'))
        jvmVersion.split(".").map { it.toInt() }
    } catch (e: Exception) {
        fail("Failed to obtain version digits: $e")
        throw e
    }

    if (versionDigits[1] < 8) {
        fail("Outdated JVM: Java 8 or later required (got ${versionDigits.joinToString(".")})")
    }

    val is64bit = errorOutput.lines()[2].contains("64-Bit")
    if (!is64bit) {
        warn("This JVM doesn't appear to be 64-Bit compliant. No support will be offered for such configurations.")
    }

    return JvmInfo(jvmType, versionDigits[0], versionDigits[1], versionDigits[2], is64bit)
}
