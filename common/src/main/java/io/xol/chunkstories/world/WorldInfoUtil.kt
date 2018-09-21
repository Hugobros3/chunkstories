package io.xol.chunkstories.world

import com.google.gson.Gson
import io.xol.chunkstories.api.world.WorldInfo
import java.io.File

fun deserializeWorldInfo(file: File) : WorldInfo {
    val contents = file.readText()

    val gson = Gson()
    return gson.fromJson(contents, WorldInfo::class.java)
}