package xyz.chunkstories.world

import com.google.gson.Gson
import xyz.chunkstories.api.world.World
import java.io.File
import java.io.IOException

data class WorldInternalData(
        var ticksCounter: Long = 0L,
        var nextEntityId: Long = 0L,

        var sky: World.Sky = World.Sky(timeOfDay = 0.3f, overcast = 0.05f, raining = 0.0f),
        /** Time (in seconds) per complete cycle */
        var dayNightCycleDuration: Double = 60.0 * 20.0,
        var varyWeather: Boolean = true
)

internal const val worldInternalDataFilename = "internalData.json"

fun tryLoadWorldInternalData(folder: File) : WorldInternalData {
    val file = File("${folder.path}/$worldInternalDataFilename")
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

fun WorldMasterImplementation.saveInternalData() {
    val file = File("$folderPath/$worldInternalDataFilename")
    val gson = Gson()
    val contents = gson.toJson(super_.internalData)
    file.writeText(contents)
}