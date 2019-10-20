package xyz.chunkstories.launcher

import java.io.File
import java.util.*

data class PreLaunchOptions(val dedicatedRam: Int = 2048)

fun loadOrDefaultPreLaunchOptions(): PreLaunchOptions {
    val existingConfig = File("config/client.config")
    return if (existingConfig.exists()) {
        val properties = Properties()
        properties.load(existingConfig.reader())
        val dedicatedRam = (properties["client.performance.dedicatedRam"] as? String)?.toIntOrNull() ?: 2048

        val vd = (properties["client.graphics.viewDistance"] as? String)?.toIntOrNull() ?: 2048
        println("kek: $vd")

        PreLaunchOptions(dedicatedRam)
    } else {
        PreLaunchOptions()
    }
}