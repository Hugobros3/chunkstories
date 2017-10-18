package io.xol.chunkstories.workers;

import io.xol.chunkstories.api.workers.Task;
import io.xol.chunkstories.api.workers.TaskExecutor;
import io.xol.chunkstories.api.workers.Tasks;

public class WorkerThreadPool extends TasksPool<Task> implements Tasks
{
	protected int threadsCount;
	protected WorkerThread[] workers;
	
	public WorkerThreadPool(int threadsCount)
	{
		this.threadsCount = threadsCount;
	}
	
	public void start() {
		workers = new WorkerThread[threadsCount];
		for(int id = 0; id < threadsCount; id++)
			workers[id] = spawnWorkerThread(id);
	}
	
	protected WorkerThread spawnWorkerThread(int id) {
		return new WorkerThread(this, id);
	}
	
	//Virtual task the reference is used to signal threads to end.
	protected Task DIE = new Task() {

		@Override
		protected boolean task(TaskExecutor whoCares)
		{
			return true;
		}
		
	};
	
	long tasksRan = 0;
	long tasksRescheduled = 0;
	
	void rescheduleTask(Task task)
	{
		tasksQueue.add(task);
		tasksCounter.release();
		
		tasksRescheduled++;
	}
	
	public String toString() {
		return "[WorkerThreadPool threadCount="+this.threadsCount+", tasksRan="+tasksRan+", tasksRescheduled="+tasksRescheduled+"]";
	}
	
	public void destroy()
	{
		//Send threadsCount DIE orders
		for(int i = 0; i < threadsCount; i++)
			this.scheduleTask(DIE);
	}

	@Override
	public int submittedTasks() {
		return this.tasksQueueSize.get();
	}
}
