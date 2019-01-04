package io.xol.chunkstories.world.chunk.deriveddata

import io.xol.chunkstories.api.GameContext
import io.xol.chunkstories.api.util.concurrency.Fence
import io.xol.chunkstories.api.workers.Task
import io.xol.chunkstories.api.workers.TaskExecutor
import io.xol.chunkstories.util.concurrency.TrivialFence
import java.util.concurrent.locks.ReentrantLock

/**
 * Holds some derived data with high computational time, that uses a task to execute the rebuilding process.
 * Maintains a state of updates that are yet to be taken into account.
 * Examples: Chunk lighting, occlusion, meshes, heightmap metadata, etc.
 * */
abstract class AutoRebuildingProperty(val context: GameContext, initializeClean: Boolean) {
    protected val lock = ReentrantLock()
    protected var pendingUpdates = if(initializeClean) 0 else 1

    protected var task : UpdateTask? = null
    internal var isDestroyed = false

    init {
        spawnTaskIfNeeded()
    }

    internal fun spawnTaskIfNeeded() : Task? {
        try {
            lock.lock()
            if(pendingUpdates > 0 && canSpawnTask()) {
                val updatesToConsider = pendingUpdates
                //pendingUpdates = 0

                //println("updates waiting: $updatesToConsider $this")
                task = createTask(updatesToConsider)
                context.tasks.scheduleTask(task)
            }
            return null
        } finally {
            lock.unlock()
        }
    }

    //TODO in practice checking the state will go unused because the task cleans up it's own reference
    internal fun canSpawnTask() = !isDestroyed && (task == null || task?.state == Task.State.DONE || task?.state == Task.State.CANCELLED)

    abstract fun createTask(updatesToConsider: Int) : UpdateTask

    fun requestUpdateAndGetFence() : Fence {
        pendingUpdates++
        var task = spawnTaskIfNeeded()
        task = task ?: this.task
        return task ?: TrivialFence()
    }

    fun requestUpdate() {
        pendingUpdates++
        spawnTaskIfNeeded()
    }

    abstract class UpdateTask(val attachedProperty: AutoRebuildingProperty, private val updates: Int) : Task() {
        /** UpdateTask note: Override update() instead */
        final override fun task(taskExecutor: TaskExecutor): Boolean {
            // Possible optimization: Cancel the task outright so we can consider more new updates

            if(!update(taskExecutor))
                return false

            try {
                attachedProperty.lock.lock()
                //println("removing $updates from ${attachedProperty.pendingUpdates}")
                attachedProperty.pendingUpdates -= updates
                attachedProperty.task = null

                attachedProperty.spawnTaskIfNeeded()
                return true
            } finally {
                attachedProperty.lock.unlock()
            }
        }

        abstract fun update(taskExecutor: TaskExecutor): Boolean
    }

    open fun cleanup() = Unit

    fun destroy() {
        try {
            lock.lock()
            task?.tryCancel()
            cleanup()
            isDestroyed = true
        } finally {
            lock.unlock()
        }
    }
}