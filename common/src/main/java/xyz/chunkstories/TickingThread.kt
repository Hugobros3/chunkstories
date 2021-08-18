//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories

import java.util.concurrent.atomic.AtomicBoolean

import org.slf4j.LoggerFactory
import xyz.chunkstories.api.plugin.Plugin
import xyz.chunkstories.api.plugin.Scheduler

import xyz.chunkstories.api.util.concurrency.Fence
import xyz.chunkstories.util.concurrency.SimpleFence
import xyz.chunkstories.util.concurrency.TrivialFence
import xyz.chunkstories.world.WorldImplementation
import java.util.ArrayList
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.Semaphore

const val TPS = 60

class SchedulerImpl : Scheduler {
    private val scheduledTasks: MutableList<ScheduledTask> = ArrayList()
    fun runScheduledTasks() {
        try {
            scheduledTasks.removeIf { obj: ScheduledTask -> !obj.tick() }
        } catch (t: Throwable) {
            logger.error(t.message)
            t.printStackTrace()
        }
    }

    override fun scheduleSyncRepeatingTask(plugin: Plugin, runnable: Runnable, delay: Long, period: Long) {
        scheduledTasks.add(ScheduledTask(runnable, delay, period))
    }

    internal inner class ScheduledTask(var runnable: Runnable, var delay: Long, var period: Long) {
        fun tick(): Boolean {
            if (--delay > 0)
                return true
            runnable.run()
            if (period <= 0)
                return false
            delay = period
            return true
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger("scheduler")
    }
}

abstract class TickingThread(private val world: WorldImplementation) : Thread() {
    private val scheduler: SchedulerImpl = SchedulerImpl()

    private val die = AtomicBoolean(false)

    private val waitForLogicFinish = SimpleFence()

    var lastNano: Long = 0
        private set
    var tps = 0.0f
        private set
    var lastTime: Long = 0
        private set
    var lastTimeNs: Long = 0
        private set

    val targetTps: Int
        get() = TPS

    val simulationFps: Double
        get() = Math.floor((tps * 100f).toDouble()) / 100f

    val simulationSpeed: Double
        get() = 1.0

    private fun nanoCheckStep(maxNs: Int, warn: String) {
        val took = System.nanoTime() - lastNano
        // Took more than n ms ?
        if (took > maxNs * 1000000)
            logger.warn(warn + " " + (took / 1000L).toDouble() / 1000 + "ms")

        lastNano = System.nanoTime()
    }

    override fun run() {
        while (!die.get()) {
            // Dirty performance metric :]
            // perfMetric();
            // nanoCheckStep(20, "Loop was more than 20ms");

            // Timings
            tps = 1f / ((System.nanoTime() - lastTimeNs).toFloat() / 1000f / 1000f / 1000f)
            lastTimeNs = System.nanoTime()

            tick()

            scheduler.runScheduledTasks()

            sync(targetTps)
        }

        waitForLogicFinish.signal()
    }

    fun tickWorld() {
        try {
            world.tick()
        } catch (e: Exception) {
            world.logger.error("Exception occurred while ticking the world : ", e)
        }
    }

    abstract fun tick()

    val lastTimeInMs: Float
        get() = (((System.nanoTime() - lastTimeNs) / 1000.0) / 1000.0).toFloat()

    /** fancy sync method from SO iirc  */
    fun sync(fps: Int) {
        if (fps <= 0)
            return

        val errorMargin = (1000 * 1000).toLong() // 1 millisecond error margin for  Thread.sleep()
        val sleepTime = (1000000000 / fps).toLong() // nanoseconds to sleep this frame

        // if smaller than sleepTime burn for errorMargin + remainder micro &
        // nano seconds
        val burnTime = Math.min(sleepTime, errorMargin + sleepTime % (1000 * 1000))

        var overSleep: Long = 0 // time the sleep or burn goes over by

        try {
            while (true) {
                val t = System.nanoTime() - lastTime

                if (t < sleepTime - burnTime) {
                    sleep(1)
                } else if (t < sleepTime) {
                    // burn the last few CPU cycles to ensure accuracy
                    yield()
                } else {
                    overSleep = Math.min(t - sleepTime, errorMargin)
                    break // exit while loop
                }
            }
        } catch (e: InterruptedException) {
        }

        lastTime = System.nanoTime() - overSleep
    }

    fun terminate(): Fence {
        if (!this.isAlive)
            return TrivialFence()

        die.set(true)

        return waitForLogicFinish
    }

    val logicThread: Thread = currentThread()

    /** Some actions can only execute on the main thread */
    private val actionsQueue = ConcurrentLinkedDeque<ScheduledAction>()

    data class ScheduledAction(val action: () -> Unit) {
        val semaphore = Semaphore(0)
    }

    /** Schedules some work to be executed on the main thread */
    fun logicThread(function: () -> Unit) {
        if (currentThread() == logicThread) {
            function()
        } else {
            actionsQueue.addLast(ScheduledAction(function))
        }
    }

    fun logicThreadBlocking(function: () -> Unit) {
        if (currentThread() == logicThread) {
            function()
        } else {
            val scheduled = ScheduledAction(function)
            actionsQueue.addLast(scheduled)
            scheduled.semaphore.acquireUninterruptibly()
        }
    }
    companion object {
        private val logger = LoggerFactory.getLogger("gameLogic")
    }
}
