package io.xol.chunkstories.workers;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

/**
 * A task pool receives tasks and dispatches them
 */
public abstract class TasksPool<T extends Task>
{
	Queue<T> tasksQueue = new ConcurrentLinkedQueue<T>();
	Semaphore tasksCounter = new Semaphore(0);
	
	public void scheduleTask(T task)
	{
		tasksQueue.add(task);
		tasksCounter.release();
	}
}
