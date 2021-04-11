//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.world.io;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.chunkstories.api.workers.TaskExecutor;
import xyz.chunkstories.api.world.World;

/** A shitty task system to deal with I/O */
public class IOTasks extends Thread implements TaskExecutor {
	protected World world;

	public final Deque<IOTask> tasks = new ConcurrentLinkedDeque<>();
	private final Semaphore tasksCounter = new Semaphore(0);

	private IOTask DIE = new IOTask() {

		@Override
		protected boolean task(TaskExecutor taskExecutor) {
			return false;
		}
	};

	public IOTasks(World world) {
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
		logger().info("IO Thread started for '" + this.world.getProperties().getName() + "'");

		this.setName("IO thread for '" + this.world.getProperties().getName() + "'");
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
					if (!taskSuccessfull)
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
		for (IOTask task : this.tasks) {
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
