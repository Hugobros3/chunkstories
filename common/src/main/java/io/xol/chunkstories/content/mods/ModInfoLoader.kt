package io.xol.chunkstories.content.mods

import com.google.gson.Gson
import io.xol.chunkstories.api.content.mods.ModInfo
import java.io.Reader

fun loadModInfo(reader: Reader) : ModInfo {
    val gson = Gson()

    return gson.fromJson(reader, ModInfo::class.java)
}