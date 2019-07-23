package xyz.chunkstories.serialization

import com.google.gson.Gson
import junit.framework.Assert.assertTrue
import org.junit.Test
import xyz.chunkstories.world.WorldInternalData

class TestWorldInternalData {

    @Test
    fun testJsonSerialization() {
        val gson = Gson()
        val start = WorldInternalData()
        start.spawnLocation.set(42.0, 69.0, 0.3333)
        val serialized = gson.toJson(start)
        println(serialized)
        val end = gson.fromJson(serialized, WorldInternalData::class.java)
        assertTrue(start == end)
        println(end)
    }
}