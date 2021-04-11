package xyz.chunkstories.world

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import xyz.chunkstories.api.world.World
import java.io.File

fun deserializeWorldInfo(file: File): World.Properties = deserializeWorldInfo(file.readText())

fun deserializeWorldInfo(contents: String): World.Properties {
    val gson = Gson()
    return gson.fromJson(contents, World.Properties::class.java)
}

fun serializeWorldInfo(worldInfo: World.Properties, pretty: Boolean): String {
    val gsonBuilder = GsonBuilder()
    if (pretty) gsonBuilder.setPrettyPrinting()

    val gson = Gson()
    return gson.toJson(worldInfo)
}