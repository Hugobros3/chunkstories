//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.util

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

object CPUModelDetection {
    fun detectModel(): String {
        var command = ""

        var cpuName = ""
        var cpuFreq = "unknown"

        when(OSHelper.os) {
            SupportedOS.LINUX -> command = "cat /proc/cpuinfo"
            SupportedOS.WINDOWS -> command = "cmd /C WMIC CPU Get /Format:List <NUL"
            else -> return "Mac, not implemented, workarround"
        }

        var process: Process? = null
        try {
            process = Runtime.getRuntime().exec(command)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        var cores = 0

        val reader = BufferedReader(InputStreamReader(process!!.inputStream))
        val text = reader.readText()

        for(line in text.lines()) {
            if (line.startsWith("Name="))
                cpuName = line.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
            if (line.startsWith("model name")) {
                cpuName = line.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
                // On linux we count cores
                cores++
            }
            if (line.startsWith("NumberOfCores="))
                cores += Integer.parseInt(line.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])

            if (line.startsWith("CurrentClockSpeed"))
                cpuFreq = line.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
            if (line.startsWith("cpu MHz"))
                cpuFreq = line.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
        }

        return cores.toString() + "x " + cpuName + " @ " + cpuFreq + "MHz"
    }

}
