//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.workers;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import io.xol.chunkstories.api.workers.Task;

/**
 * A task pool receives tasks and dispatches them
 */
public abstract class TasksPool<T extends Task>
{
	protected Deque<T> tasksQueue = new ConcurrentLinkedDeque<T>();
	protected Semaphore tasksCounter = new Semaphore(0);
	protected AtomicInteger tasksQueueSize = new AtomicInteger(0);
	
	public void scheduleTask(T task)
	{
		//Add the tasks BEFORE doing the thing
		tasksQueue.add(task);
		tasksCounter.release();
		tasksQueueSize.incrementAndGet();
	}
	
	public int size()
	{
		return tasksQueueSize.get();
	}
}
