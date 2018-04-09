//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.logic;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.Constants;
import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.GameLogic;
import io.xol.chunkstories.api.events.world.WorldTickEvent;
import io.xol.chunkstories.api.plugin.PluginManager;
import io.xol.chunkstories.api.plugin.Scheduler;
import io.xol.chunkstories.api.util.concurrency.Fence;
import io.xol.chunkstories.util.concurrency.SimpleFence;
import io.xol.chunkstories.util.concurrency.TrivialFence;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.region.RegionImplementation;

/**
 * Sandboxed thread that runs all the game logic for one world
 */
//TODO actually sandbox it lol
public class WorldLogicThread extends Thread implements GameLogic {
	private final GameContext context;

	private final WorldImplementation world;

	private GameLogicScheduler gameLogicScheduler;

	private AtomicBoolean die = new AtomicBoolean(false);

	private SimpleFence waitForLogicFinish = new SimpleFence();

	private static final Logger logger = LoggerFactory.getLogger("worldlogic");

	public WorldLogicThread(WorldImplementation world, SecurityManager securityManager) {
		this.world = world;
		this.context = world.getGameContext();

		this.setName("World '" + world.getWorldInfo().getInternalName() + "' logic thread");
		this.setPriority(Constants.MAIN_SINGLEPLAYER_LOGIC_THREAD_PRIORITY);

		gameLogicScheduler = new GameLogicScheduler();

		// this.start();
	}

	public GameContext getGameContext() {
		return context;
	}

	long lastNano;

	@SuppressWarnings("unused")
	private void nanoCheckStep(int maxNs, String warn) {
		long took = System.nanoTime() - lastNano;
		// Took more than n ms ?
		if (took > maxNs * 1000000)
			logger.warn(warn + " " + (double) (took / 1000L) / 1000 + "ms");

		lastNano = System.nanoTime();
	}

	public void run() {
		// TODO
		// Installs a custom SecurityManager
		logger.info("Security manager: " + System.getSecurityManager());

		while (!die.get()) {
			// Dirty performance metric :]
			// perfMetric();
			// nanoCheckStep(20, "Loop was more than 20ms");

			// Timings
			fps = 1f / ((System.nanoTime() - lastTimeNs) / 1000f / 1000f / 1000f);
			lastTimeNs = System.nanoTime();

			this.getPluginsManager().fireEvent(new WorldTickEvent(world));

			try {
				world.tick();
			} catch (Exception e) {
				world.logger().error("Exception occured while ticking the world : ", e);
			}

			// nanoCheckStep(5, "Tick");

			// Every second, unloads unused stuff
			if (world.getTicksElapsed() % 60 == 0) {
				// System.gc();

				// Compresses pending chunk summaries
				Iterator<RegionImplementation> loadedChunksHolders = world.getRegionsHolder().internalGetLoadedRegions();
				while (loadedChunksHolders.hasNext()) {
					RegionImplementation region = loadedChunksHolders.next();
					region.compressChangedChunks();
				}

				// Delete unused world data
				// world.unloadUselessData();
			}
			// nanoCheckStep(1, "unload");

			gameLogicScheduler.runScheduledTasks();
			// nanoCheckStep(1, "schedule");

			// Game logic is 60 ticks/s
			sync(getTargetFps());
		}

		waitForLogicFinish.signal();
	}

	float fps = 0.0f;

	long lastTime = 0;
	long lastTimeNs = 0;

	@Override
	public int getTargetFps() {
		return 60;
	}

	public double getSimulationFps() {
		return (double) (Math.floor(fps * 100f) / 100f);
	}

	public double getSimulationSpeed() {
		return 1.0;
	}

	public void perfMetric() {
		double ms = Math.floor((System.nanoTime() - lastTimeNs) / 10000.0) / 100.0;
		String kek = "";
		double d = 0.02;
		for (double i = 0; i < 1.0; i += d)
			kek += Math.abs(ms - 16 - i) > d ? " " : "|";
		System.out.println(kek + ms + "ms");
	}

	/** fancy sync method from SO iirc */
	public void sync(int fps) {
		if (fps <= 0)
			return;

		long errorMargin = 1000 * 1000; // 1 millisecond error margin for
										// Thread.sleep()
		long sleepTime = 1000000000 / fps; // nanoseconds to sleep this frame

		// if smaller than sleepTime burn for errorMargin + remainder micro &
		// nano seconds
		long burnTime = Math.min(sleepTime, errorMargin + sleepTime % (1000 * 1000));

		long overSleep = 0; // time the sleep or burn goes over by

		try {
			while (true) {
				long t = System.nanoTime() - lastTime;

				if (t < sleepTime - burnTime) {
					Thread.sleep(1);
				} else if (t < sleepTime) {
					// burn the last few CPU cycles to ensure accuracy
					Thread.yield();
				} else {
					overSleep = Math.min(t - sleepTime, errorMargin);
					break; // exit while loop
				}
			}
		} catch (InterruptedException e) {
		}

		lastTime = System.nanoTime() - overSleep;
	}

	@Override
	public PluginManager getPluginsManager() {
		return context.getPluginManager();
	}

	public Fence stopLogicThread() {
		if (!this.isAlive())
			return new TrivialFence();

		die.set(true);

		return waitForLogicFinish;
	}

	@Override
	public Scheduler getScheduler() {
		return gameLogicScheduler;
	}
}
