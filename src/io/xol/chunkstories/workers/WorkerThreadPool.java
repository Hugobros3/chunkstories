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
		boolean runTask()
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
				tasksCounter.acquireUninterruptibly();
				Task task = tasksQueue.poll();
				
				assert task != null;
				
				//Only die task can break the loop
				if(task == DIE)
					break;
				
				task.run();
			}
		}
	}
	
	public void destroy()
	{
		//Send threadsCount DIE orders
		for(int i = 0; i < threadsCount; i++)
			this.scheduleTask(DIE);
	}
}
