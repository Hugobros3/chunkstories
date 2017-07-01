package io.xol.chunkstories.workers;

public class WorkerThreadPool extends TasksPool<Task>
{
	private int threadsCount;
	private WorkerThread[] workers;
	
	public WorkerThreadPool(int threadsCount)
	{
		this.threadsCount = threadsCount;
		
		workers = new WorkerThread[threadsCount];
		for(int i = 0; i < threadsCount; i++)
			workers[i] = new WorkerThread(i);
	}
	
	//Virtual task the reference is used to signal threads to end.
	Task DIE = new Task() {

		@Override
		protected boolean task()
		{
			return true;
		}
		
	};
	
	class WorkerThread extends Thread {
		
		WorkerThread(int id)
		{
			this.setName("Worker thread #"+id);
			this.start();
		}
		
		public void run()
		{
			while(true)
			{
				//Aquire a work permit
				tasksCounter.acquireUninterruptibly();
				
				//If one such permit was found to exist, assert a task is readily avaible
				Task task = tasksQueue.poll();
				
				assert task != null;
				
				//Only die task can break the loop
				if(task == DIE)
					break;
				
				boolean result = task.run();
				tasksRan++;
				
				//Depending on the result we either reschedule the task or decrement the counter
				if(result == false)
					rescheduleTask(task);
				else
					tasksQueueSize.decrementAndGet();
			}
		}
	}
	
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
}
