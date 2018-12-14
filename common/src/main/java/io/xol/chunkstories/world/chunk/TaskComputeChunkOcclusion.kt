//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.chunk

import java.util.HashSet

import com.carrotsearch.hppc.IntArrayDeque
import com.carrotsearch.hppc.IntDeque

import io.xol.chunkstories.api.workers.Task
import io.xol.chunkstories.api.workers.TaskExecutor

class TaskComputeChunkOcclusion {

    internal val chunk: CubicChunk? = null

    private fun computeOcclusionTable(): Array<BooleanArray> {
        val occlusionSides = Array(6) { BooleanArray(6) }

        val deque = occlusionFaces.get()
        deque.clear()

        val mask = masks.get()
        boolfill(mask, false)

        var x = 0
        var y = 0
        var z = 0
        var completion = 0
        var p = 0

        var bits = 0
        // Until all 32768 blocks have been processed
        while (completion < 32768) {
            // If this face was already done, we find one that wasn't
            while (mask[x * 1024 + y * 32 + z]) {
                p++
                p %= 32768

                x = p / 1024
                y = p / 32 % 32
                z = p % 32
            }

            bits++

            // We put this face on the deque
            deque.addLast(x * 1024 + y * 32 + z)

            /**
             * Conventions for space in Chunk Stories 1 FRONT z+ x- LEFT 0 X 2 RIGHT x+ 3
             * BACK z- 4 y+ top X 5 y- bottom
             */
            val touchingSides = HashSet<Int>()
            while (!deque.isEmpty) {
                // Pop the topmost element
                val d = deque.removeLast()

                // Don't iterate twice over one element
                if (mask[d])
                    continue

                // Separate coordinates
                x = d / 1024
                y = d / 32 % 32
                z = d % 32

                // Mark the case as done
                mask[x * 1024 + y * 32 + z] = true
                completion++

                if (!chunk!!.peekSimple(x, y, z).opaque) {
                    // Adds touched sides to set

                    if (x == 0)
                        touchingSides.add(0)
                    else if (x == 31)
                        touchingSides.add(2)

                    if (y == 0)
                        touchingSides.add(5)
                    else if (y == 31)
                        touchingSides.add(4)

                    if (z == 0)
                        touchingSides.add(3)
                    else if (z == 31)
                        touchingSides.add(1)

                    // Flood fill

                    if (x > 0)
                        deque.addLast((x - 1) * 1024 + y * 32 + z)
                    if (y > 0)
                        deque.addLast(x * 1024 + (y - 1) * 32 + z)
                    if (z > 0)
                        deque.addLast(x * 1024 + y * 32 + (z - 1))

                    if (x < 31)
                        deque.addLast((x + 1) * 1024 + y * 32 + z)
                    if (y < 31)
                        deque.addLast(x * 1024 + (y + 1) * 32 + z)
                    if (z < 31)
                        deque.addLast(x * 1024 + y * 32 + (z + 1))
                }
            }

            for (i in touchingSides) {
                for (j in touchingSides)
                    occlusionSides[i][j] = true
            }
        }

        return occlusionSides
    }

    companion object {

        internal var occlusionFaces: ThreadLocal<IntDeque> = object : ThreadLocal<IntDeque>() {
            override fun initialValue(): IntDeque {
                return IntArrayDeque()
            }
        }

        internal var masks: ThreadLocal<BooleanArray> = object : ThreadLocal<BooleanArray>() {

            override fun initialValue(): BooleanArray {
                return BooleanArray(32768)
            }

        }

        /*
	 * initialize a smaller piece of the array and use the System.arraycopy call to
	 * fill in the rest of the array in an expanding binary fashion
	 */
        fun boolfill(array: BooleanArray, value: Boolean) {
            val len = array.size

            if (len > 0) {
                array[0] = value
            }

            var i = 1
            while (i < len) {
                System.arraycopy(array, 0, array, i, if (len - i < i) len - i else i)
                i += i
            }
        }
    }

}
