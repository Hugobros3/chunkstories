//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//
package xyz.chunkstories.task

import xyz.chunkstories.api.workers.Task
import java.util.Deque
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicInteger

abstract class TasksPool<T : Task?> {
    internal var tasksQueue: Deque<T> = ConcurrentLinkedDeque()
    internal var tasksCounter = Semaphore(0)
    internal var tasksQueueSize = AtomicInteger(0)

    fun scheduleTask(task: T) {
        tasksQueue.add(task)
        tasksCounter.release()
        tasksQueueSize.incrementAndGet()
    }

    fun size(): Int {
        return tasksQueueSize.get()
    }
}