package io.xol.chunkstories.workers;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A task pool receives tasks and dispatches them
 */
public abstract class TasksPool<T extends Task>
{
	protected Queue<T> tasksQueue = new ConcurrentLinkedQueue<T>();
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
