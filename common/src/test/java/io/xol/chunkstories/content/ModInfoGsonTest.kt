package io.xol.chunkstories.content

import com.google.gson.Gson
import io.xol.chunkstories.api.content.mods.ModInfo
import org.junit.Test

class ModInfoGsonTest {
    val sample = """
        {
            "internalName" : "core",
            "name": "Chunk Stories default content",
            "version": "non-pbr indev"
        }
    """.trimIndent()

    @Test
    fun testDeserializingModInfoWithGson() {
        val gson = Gson()

        val modInfo : ModInfo = gson.fromJson(sample, ModInfo::class.java)
        println(modInfo)

        val modInfo2 = ModInfo("eh", "Eh !", "1.0.1", "Eeeeh !!!")
    }
}