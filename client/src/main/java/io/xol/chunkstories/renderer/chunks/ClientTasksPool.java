//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.renderer.chunks;

import java.util.concurrent.atomic.AtomicInteger;

import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.task.WorkerThread;
import io.xol.chunkstories.task.WorkerThreadPool;

public class ClientTasksPool extends WorkerThreadPool {

	protected AtomicInteger totalChunksRendered = new AtomicInteger();
	protected final ClientInterface client;
	
	///private int threadsCount;
	//private ClientWorkerThread[] workers;
	
	public ClientTasksPool(ClientInterface client, int threadsCount)
	{
		super(threadsCount);
		this.client = client;
		//this.threadsCount = threadsCount;
		
		//workers = new ClientWorkerThread[threadsCount];
		//for(int i = 0; i < threadsCount; i++)
		//	workers[i] = new ClientWorkerThread(i);
	}
	
	
	
	//Virtual task the reference is used to signal threads to end.
	/*Task DIE = new Task() {

		@Override
		protected boolean task(TaskExecutor task)
		{
			return true;
		}
		
	};*/
	
	@Override
	protected WorkerThread spawnWorkerThread(int id) {
		return new ClientWorkerThread(this, id);
	}

	/*long tasksRan = 0;
	long tasksRescheduled = 0;

	public void destroy()
	{
		//Send threadsCount DIE orders
		for(int i = 0; i < threadsCount; i++)
			this.scheduleTask(DIE);
	}*/
	
	@Override
	public String toString() {
		return "[ClientTasksPool threads:"+threadsCount+" tasksQueue: "+tasksQueue.size()+" ]";
	}
	
}
