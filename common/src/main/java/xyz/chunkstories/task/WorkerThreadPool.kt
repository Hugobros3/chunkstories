//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.task

import xyz.chunkstories.api.workers.Task
import xyz.chunkstories.api.workers.TaskExecutor
import xyz.chunkstories.api.workers.Tasks
import org.slf4j.Logger
import org.slf4j.LoggerFactory

open class WorkerThreadPool(protected var threadsCount: Int) : TasksPool<Task>(), Tasks {
    val pending: Collection<Task>
        get() = super.tasksQueue

    protected lateinit var workers: Array<WorkerThread>

    // Virtual task the reference is used to signal threads to end.
    internal var DIE: Task = object : Task() {

        override fun task(whoCares: TaskExecutor): Boolean {
            return true
        }

    }

    internal var tasksRan: Long = 0
    internal var tasksRescheduled: Long = 0

    fun start() {
        workers = Array(threadsCount) {
            spawnWorkerThread(it)
        }
    }

    protected fun spawnWorkerThread(id: Int): WorkerThread {
        return WorkerThread(this, id)
    }

    internal fun rescheduleTask(task: Task) {
        tasksQueue.addLast(task)
        tasksCounter.release()

        tasksRescheduled++
    }

    override fun toString(): String {
        return ("[WorkerThreadPool threadCount=" + this.threadsCount + ", tasksRan=" + tasksRan + ", tasksRescheduled="
                + tasksRescheduled + "]")
    }

    fun toShortString(): String {
        return "workers tc: " + this.threadsCount + ", todo: " + submittedTasks() + ""
    }

    fun cleanup() {
        // Send threadsCount DIE orders
        for (i in 0 until threadsCount)
            this.scheduleTask(DIE)
    }

    override fun submittedTasks(): Int {
        return this.tasksQueueSize.get()
    }

    fun dumpTasks() {
        println("dumping tasks")

        // a security because you can fill the queue faster than you can iterate on it
        var antiInfiniteLoop = 500
        val i = this.tasksQueue.iterator()
        while (i.hasNext()) {
            val task = i.next()
            antiInfiniteLoop--
            if (antiInfiniteLoop < 0)
                return
            println(task)
        }
    }

    fun logger(): Logger {
        return logger
    }

    companion object {
        private val logger = LoggerFactory.getLogger("workers")
    }
}
