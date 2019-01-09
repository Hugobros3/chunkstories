package xyz.chunkstories.content.mods

import com.google.gson.Gson
import xyz.chunkstories.api.content.mods.ModInfo
import java.io.Reader

fun loadModInfo(reader: Reader) : ModInfo {
    val gson = Gson()

    return gson.fromJson(reader, ModInfo::class.java)
}