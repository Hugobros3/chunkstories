package xyz.chunkstories.world

import com.google.gson.Gson
import org.joml.Vector3d
import java.io.File
import java.io.IOException

data class WorldInternalData(
        val spawnLocation: Vector3d = Vector3d(128.0, 64.0, 128.0),
        var nextEntityId: Long = 0L,
        var ticksCounter: Long = 0L,

        var sunCycleTime: Int = 6000,
        var dayNightCycleSpeed: Int = 1,

        var weather: Float = 0.25f,
        var varyWeather: Boolean = true
)

fun loadInternalDataFromDisk(file: File) : WorldInternalData {
    if(file.exists()) {
        try {
            val contents = file.readText()
            val gson = Gson()
            return gson.fromJson(contents, WorldInternalData::class.java)
        } catch (e: IOException) {

        }
    }

    return WorldInternalData()
}

fun WorldInternalData.writeToDisk(file: File) {
    val gson = Gson()
    val contents = gson.toJson(this)
    file.writeText(contents)
}