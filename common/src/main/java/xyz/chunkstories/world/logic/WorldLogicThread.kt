//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.world.logic

import java.util.concurrent.atomic.AtomicBoolean

import org.slf4j.LoggerFactory

import xyz.chunkstories.ThreadPriorities
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

/**
 * Sandboxed thread that runs all the game logic for one world
 */
//TODO actually sandbox it lol
class WorldLogicThread(private val world: WorldImplementation, securityManager: SecurityManager) : Thread() {
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
        this.name = "World '" + world.properties.internalName + "' logic thread"
        this.priority = ThreadPriorities.MAIN_SINGLEPLAYER_LOGIC_THREAD_PRIORITY

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
        // TODO
        // Installs a custom SecurityManager
        logger.info("Security manager: " + System.getSecurityManager())

        while (!die.get()) {
            // Dirty performance metric :]
            // perfMetric();
            // nanoCheckStep(20, "Loop was more than 20ms");

            // Timings
            fps = 1f / ((System.nanoTime() - lastTimeNs).toFloat() / 1000f / 1000f / 1000f)
            lastTimeNs = System.nanoTime()

            world.gameInstance.pluginManager.fireEvent(WorldTickEvent(world))

            try {
                world.tick()
            } catch (e: Exception) {
                world.logger.error("Exception occured while ticking the world : ", e)
            }

            // nanoCheckStep(5, "Tick");

            // Every second, unloads unused stuff
            if (world.ticksElapsed % 60 == 0L) {
                // Compresses pending chunk summaries
                if (world is WorldMaster) {
                    for (region in world.regionsManager.regionsList) {
                        region.compressChangedChunks()
                    }
                }
            }

            val physicsRate = 4
            if(world is WorldMaster) {
                val players = world.players
                if (world.ticksElapsed % physicsRate == 0L) {
                    for (chunk in world.chunksManager.allLoadedChunks) {
                        var minDistance = Double.MAX_VALUE
                        val chunkLocation = Location(world, chunk.chunkX * 32.0 + 16.0, chunk.chunkY * 32.0 + 16.0, chunk.chunkZ * 32.0 + 16.0)
                        for(player in players) {
                            val playerLocation = player.entityIfIngame?.location ?: continue
                            val distance = playerLocation.distance(chunkLocation)
                            if(distance < minDistance)
                                minDistance = distance
                        }

                        if(minDistance < 32 * 2.0) {
                            chunk.tick(world.ticksElapsed / physicsRate)
                        }
                    }
                }
            }

            gameLogicScheduler.runScheduledTasks()
            // nanoCheckStep(1, "schedule");

            // Game logic is 60 ticks/s
            sync(targetFps)
        }

        waitForLogicFinish.signal()
    }

    fun perfMetric() {
        val ms = Math.floor((System.nanoTime() - lastTimeNs) / 10000.0) / 100.0
        var kek = ""
        val d = 0.02
        var i = 0.0
        while (i < 1.0) {
            kek += if (Math.abs(ms - 16.0 - i) > d) " " else "|"
            i += d
        }
        println(kek + ms + "ms")
    }

    /** fancy sync method from SO iirc  */
    fun sync(fps: Int) {
        if (fps <= 0)
            return

        val errorMargin = (1000 * 1000).toLong() // 1 millisecond error margin for
        // Thread.sleep()
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

    fun stopLogicThread(): Fence {
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

        private val logger = LoggerFactory.getLogger("worldlogic")
    }
}
