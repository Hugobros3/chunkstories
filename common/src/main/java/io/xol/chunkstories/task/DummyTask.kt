//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.task

import io.xol.chunkstories.api.workers.Task
import io.xol.chunkstories.api.workers.TaskExecutor

class DummyTask : Task() {

    internal var a: Long = 0
    internal var b: Long = 0

    init {
        a = (1 + Math.random() * (java.lang.Long.MAX_VALUE - 256)).toLong()
        b = (1 + Math.random() * (java.lang.Long.MAX_VALUE - 256)).toLong()
    }

    override fun task(task: TaskExecutor): Boolean {
        // int c = pgcd(a, b);

        while (a * b > 0) {
            if (a == b)
                break

            if (a > b)
                b -= a
            else
                a -= b

        }

        var d = 500
        while (d > 0) {
            d -= if (Math.random() > 0.5) 1 else 0
        }

        return Math.random() > 0.5
    }

    internal fun pgcd(a: Int, b: Int): Int {
        return if (a == b)
            a
        else if (a == 0)
            b
        else if (b == 0)
            a
        else if (a > b)
            pgcd(a % b, b)
        else
            pgcd(a, b % a)
    }
}
