package io.xol.chunkstories.world

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.xol.chunkstories.api.world.WorldInfo
import java.io.File

fun deserializeWorldInfo(file: File): WorldInfo = deserializeWorldInfo(file.readText())

fun deserializeWorldInfo(contents: String): WorldInfo {
    val gson = Gson()
    return gson.fromJson(contents, WorldInfo::class.java)
}

fun serializeWorldInfo(worldInfo: WorldInfo, pretty: Boolean): String {
    val gsonBuilder = GsonBuilder()
    if (pretty) gsonBuilder.setPrettyPrinting()

    val gson = Gson()
    return gson.toJson(worldInfo)
}