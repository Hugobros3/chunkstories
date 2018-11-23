//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.io;

import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.Constants;
import io.xol.chunkstories.api.util.concurrency.Fence;
import io.xol.chunkstories.api.workers.TaskExecutor;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.storage.ChunkHolderImplementation;
import io.xol.chunkstories.world.heightmap.HeightmapImplementation;
import io.xol.chunkstories.world.storage.RegionImplementation;

/**
 * This thread does I/O work in queue. Extended by IOTaskMultiplayerClient and
 * IOTaskMultiplayerServer for the client/server model.
 */
public class IOTasks extends Thread implements TaskExecutor {
	protected WorldImplementation world;

	protected final Deque<IOTask> tasks = new ConcurrentLinkedDeque<>();
	private final Semaphore tasksCounter = new Semaphore(0);

	private IOTask DIE = new IOTask() {

		@Override
		protected boolean task(TaskExecutor taskExecutor) {
			return false;
		}
	};

	public IOTasks(WorldImplementation world) {
		this.world = world;
	}

	public boolean scheduleTask(IOTask task) {
		boolean code = tasks.add(task);
		tasksCounter.release();
		return code;
	}

	@Override
	public String toString() {
		return "[IO :" + getSize() + " in queue.]";
	}

	@Override
	public void run() {
		logger().info("IO Thread started for '" + this.world.getWorldInfo().getName() + "'");

		this.setPriority(Constants.IO_THREAD_PRIOTITY);
		this.setName("IO thread for '" + this.world.getWorldInfo().getName() + "'");
		while (true) {
			IOTask task = null;

			tasksCounter.acquireUninterruptibly();

			task = tasks.poll();
			if (task == null) {
				// Crash and burn
				logger.error("Fatal: a null task was in the IOTasks queue :(");
				System.exit(-1);
			} else if (task == DIE) {
				break;
			} else {
				try {
					boolean taskSuccessfull = task.run(this);

					// If it returns false, requeue it.
					if (taskSuccessfull == false)
						rescheduleTask(task);

				} catch (Exception e) {
					logger().warn("Exception occured when processing task : " + task);
					e.printStackTrace();
				}
			}
		}
		logger.info("IOTasks worker thread stopped");
	}

	void rescheduleTask(IOTask task) {
		tasks.add(task);
		tasksCounter.release();
	}

	public int getSize() {
		int i = 0;
		synchronized (tasks) {
			i = tasks.size();
		}
		return i;
	}

	/**
	 * Loads the content of a region chunk slot
	 */
	/*public IOTaskLoadChunk requestChunkLoad(ChunkHolderImplementation chunkSlot) {
		IOTaskLoadChunk task = new IOTaskLoadChunk(chunkSlot);
		if (scheduleTask(task))
			return task;
		return null;
	}*/

	/*public Fence requestRegionLoad(RegionImplementation holder) {
		// if (!isDoneSavingRegion(holder))
		// return;

		IOTask task = new IOTaskLoadRegion(holder);
		scheduleTask(task);
		return task;
	}*/

	/*public IOTask requestRegionSave(RegionImplementation holder) {
		IOTask task = new IOTaskSaveRegion(holder);
		scheduleTask(task);
		return task;
	}

	public Fence requestHeightmapLoad(HeightmapImplementation summary) {
		IOTaskLoadHeightmap task = new IOTaskLoadHeightmap(summary);
		scheduleTask(task);

		return task;
	}

	public IOTaskSaveHeightmap requestHeightmapSave(HeightmapImplementation summary) {
		IOTaskSaveHeightmap task = new IOTaskSaveHeightmap(summary);
		scheduleTask(task);

		return task;
	}*/

	public void kill() {
		scheduleTask(DIE);
		synchronized (this) {
			notifyAll();
		}
	}

	public void waitThenKill() {
		synchronized (this) {
			notifyAll();
		}

		// Wait for it to finish what it's doing
		while (this.tasks.size() > 0) {
			try {
				sleep(150L);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		scheduleTask(DIE);
	}

	public void dumpIOTaks() {
		System.out.println("dumping io tasks");

		// Hardcoding a security because you can fill the queue faster than you can
		// iterate it
		int hardLimit = 500;
		Iterator<IOTask> i = this.tasks.iterator();
		while (i.hasNext()) {
			IOTask task = i.next();
			hardLimit--;
			if (hardLimit < 0)
				return;
			System.out.println(task);
		}
	}

	private static final Logger logger = LoggerFactory.getLogger("world.io");

	public Logger logger() {
		return logger;
	}
}
