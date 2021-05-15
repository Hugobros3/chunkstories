package xyz.chunkstories.world

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import xyz.chunkstories.api.content.json.asDict
import xyz.chunkstories.api.content.json.asString
import xyz.chunkstories.api.content.json.toJson
import xyz.chunkstories.api.world.World
import java.io.File

fun deserializeWorldInfo(file: File): World.Properties = deserializeWorldInfo(file.readText())

fun deserializeWorldInfo(contents: String): World.Properties {
    val gson = Gson()
    var properties = gson.fromJson(contents, World.Properties::class.java)
    if (properties.generator == null)
        properties = properties.copy(generator = contents.toJson().asDict?.get("generatorName")?.asString ?: throw Exception("Neither 'generator' nor 'generatorName' was set"))
    return properties
}

fun serializeWorldInfo(worldInfo: World.Properties, pretty: Boolean): String {
    val gsonBuilder = GsonBuilder()
    if (pretty) gsonBuilder.setPrettyPrinting()

    val gson = Gson()
    return gson.toJson(worldInfo)
}