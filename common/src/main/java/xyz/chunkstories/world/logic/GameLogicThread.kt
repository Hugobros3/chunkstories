//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.world.logic

import java.util.concurrent.atomic.AtomicBoolean

import org.slf4j.LoggerFactory

import xyz.chunkstories.api.Location
import xyz.chunkstories.api.events.world.WorldTickEvent
import xyz.chunkstories.api.player.entityIfIngame
import xyz.chunkstories.api.util.concurrency.Fence
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.util.concurrency.SimpleFence
import xyz.chunkstories.util.concurrency.TrivialFence
import xyz.chunkstories.world.WorldImplementation
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.Semaphore

abstract class GameLogicThread(private val world: WorldImplementation) : Thread() {
    private val gameLogicScheduler: GameLogicScheduler

    private val die = AtomicBoolean(false)

    private val waitForLogicFinish = SimpleFence()

    internal var lastNano: Long = 0

    internal var fps = 0.0f

    internal var lastTime: Long = 0
    internal var lastTimeNs: Long = 0

    val targetFps: Int
        get() = 60

    val simulationFps: Double
        get() = Math.floor((fps * 100f).toDouble()) / 100f

    val simulationSpeed: Double
        get() = 1.0

    init {
        gameLogicScheduler = GameLogicScheduler()
    }

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
            fps = 1f / ((System.nanoTime() - lastTimeNs).toFloat() / 1000f / 1000f / 1000f)
            lastTimeNs = System.nanoTime()

            tick()

            gameLogicScheduler.runScheduledTasks()

            // Game logic is 60 ticks/s
            sync(targetFps)
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
                    Thread.sleep(1)
                } else if (t < sleepTime) {
                    // burn the last few CPU cycles to ensure accuracy
                    Thread.yield()
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

    val logicThread: Thread = Thread.currentThread()

    /** Some actions can only execute on the main thread */
    private val actionsQueue = ConcurrentLinkedDeque<ScheduledAction>()

    data class ScheduledAction(val action: () -> Unit) {
        val semaphore = Semaphore(0)
    }

    /** Schedules some work to be executed on the main thread */
    fun logicThread(function: () -> Unit) {
        if (Thread.currentThread() == logicThread) {
            function()
        } else {
            actionsQueue.addLast(ScheduledAction(function))
        }
    }

    fun logicThreadBlocking(function: () -> Unit) {
        if (Thread.currentThread() == logicThread) {
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
