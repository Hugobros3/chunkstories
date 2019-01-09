package xyz.chunkstories.world

import com.carrotsearch.hppc.IntHashSet
import xyz.chunkstories.api.world.WorldSize
import org.junit.Test

class TestWorldSize {
    @Test
    fun `test world size uuid mapping`() {
        for(size in WorldSize.values()) {
            val set = IntHashSet()

            for(x in 0 until size.sizeInChunks)
                for(y in 0 until size.heightInChunks)
                    for(z in 0 until size.sizeInChunks) {
                        val key = (((x shl size.bitlengthOfVerticalChunksCoordinates) or y) shl size.bitlengthOfHorizontalChunksCoordinates) or z
                        if(!set.add(key))
                            throw Exception("Chunk coordinates $x $y $z conflict in the key/uuid ($key) mapping for size $size !!!")

                        val rx = (key shr (size.bitlengthOfHorizontalChunksCoordinates + size.bitlengthOfVerticalChunksCoordinates)) and size.maskForChunksCoordinates
                        val ry = (key shr (size.bitlengthOfHorizontalChunksCoordinates)) and (31)
                        val rz = (key) and size.maskForChunksCoordinates

                        if(rx != x || ry != y || rz != z)
                            throw Exception("Coordinate reconstruction failed for $x $y $z (key $key) mapping was: $rx $ry $rz")
                    }
        }
    }
}