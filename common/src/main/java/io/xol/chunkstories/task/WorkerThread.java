//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.task;

import io.xol.chunkstories.api.workers.Task;
import io.xol.chunkstories.api.workers.TaskExecutor;
import io.xol.chunkstories.util.concurrency.SimpleFence;

public class WorkerThread extends Thread implements TaskExecutor {

	final WorkerThreadPool pool;
	final int id;

	public final SimpleFence death = new SimpleFence();

	protected WorkerThread(WorkerThreadPool pool, int id) {
		this.pool = pool;
		this.id = id;
		this.setName("Worker thread #" + id);
		this.start();
	}

	public void run() {
		while (true) {
			// Aquire a work permit
			pool.tasksCounter.acquireUninterruptibly();

			// If one such permit was found to exist, assert a task is readily avaible
			Task task = pool.tasksQueue.poll();

			assert task != null;

			// Only die task can break the loop
			if (task == pool.DIE)
				break;

			boolean result = task.run(this);
			pool.tasksRan++;

			// Depending on the result we either reschedule the task or decrement the
			// counter
			if (!result)
				pool.rescheduleTask(task);
			else
				pool.tasksQueueSize.decrementAndGet();
		}

		death.signal();
		cleanup();
	}

	protected void cleanup() {

	}
}
