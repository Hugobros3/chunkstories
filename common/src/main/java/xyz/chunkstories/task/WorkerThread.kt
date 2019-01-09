//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.task

import xyz.chunkstories.api.workers.TaskExecutor
import xyz.chunkstories.util.concurrency.SimpleFence

class WorkerThread(internal val pool: WorkerThreadPool, internal val id: Int) : Thread(), TaskExecutor {

    val death = SimpleFence()

    init {
        this.name = "Worker thread #$id"
        this.start()
    }

    override fun run() {
        while (true) {
            // Aquire a work permit
            pool.tasksCounter.acquireUninterruptibly()

            // If one such permit was found to exist, assert a task is readily avaible
            val task = pool.tasksQueue.poll()!!

            // Only die task can break the loop
            if (task === pool.DIE)
                break

            val result = task.run(this)
            pool.tasksRan++

            // Depending on the result we either reschedule the task or decrement the
            // counter
            if (!result)
                pool.rescheduleTask(task)
            else
                pool.tasksQueueSize.decrementAndGet()
        }

        death.signal()
        cleanup()
    }

    protected fun cleanup() {

    }
}
