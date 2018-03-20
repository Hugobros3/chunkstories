//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.converter;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import io.xol.chunkstories.api.workers.Task;
import io.xol.chunkstories.api.workers.TaskExecutor;
import io.xol.chunkstories.api.workers.Tasks;
import io.xol.chunkstories.api.world.WorldInfo.WorldSize;
import io.xol.chunkstories.api.world.WorldUser;
import io.xol.chunkstories.api.world.chunk.ChunkHolder;
import io.xol.chunkstories.api.world.heightmap.Heightmap;
import io.xol.chunkstories.task.TasksPool;
import io.xol.chunkstories.util.concurrency.CompoundFence;
import io.xol.chunkstories.util.concurrency.SimpleFence;
import io.xol.chunkstories.world.WorldTool;

/** Map converter-specialized workers pool, assumes only one world ever used and provides extra handy for the execution */
public class ConverterWorkers extends TasksPool<Task> implements Tasks
{
	private final MultithreadedOfflineWorldConverter converter;
	
	public final WorldTool csWorld;
	public final WorldSize size;
	
	private final int threadsCount;
	private ConverterWorkerThread[] workers;
	
	public ConverterWorkers(MultithreadedOfflineWorldConverter converter, WorldTool csWorld, int threadsCount)
	{
		this.converter = converter;
		
		this.csWorld = csWorld;
		this.size = csWorld.getWorldInfo().getSize();
		
		this.threadsCount = threadsCount;
		
		workers = new ConverterWorkerThread[threadsCount];
		for(int i = 0; i < threadsCount; i++)
			workers[i] = new ConverterWorkerThread(i);
	}
	
	//Virtual task the reference is used to signal threads to end.
	Task DIE = new Task() {

		@Override
		protected boolean task(TaskExecutor whoCares)
		{
			return true;
		}
		
	};
	
	class ConverterWorkerThread extends Thread implements TaskExecutor, WorldUser {
		
		AtomicBoolean pleaseDrop = new AtomicBoolean(false);
		
		Set<ChunkHolder> registeredCS_Holders = new HashSet<ChunkHolder>();
		Set<Heightmap> registeredCS_Summaries = new HashSet<Heightmap>();
		
		int chunksAquired = 0;
		
		ConverterWorkerThread(int id)
		{
			this.setName("Worker thread #"+id);
			this.start();
		}
		
		WorldTool world() {
			return csWorld;
		}
		
		WorldSize size() {
			return size;
		}
		
		MultithreadedOfflineWorldConverter converter() {
			return converter;
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
				
				boolean result = task.run(this);
				tasksRan++;
				
				//Depending on the result we either reschedule the task or decrement the counter
				if(result == false)
					rescheduleTask(task);
				else
					tasksQueueSize.decrementAndGet();
				
				//We have a security to prevent gobbling up too much ram
				//Also serves as a mechanism to clear loaded data when finishing a step.
				if (chunksAquired > converter().targetChunksToKeepInRam || pleaseDrop.compareAndSet(true, false))
				{
					//Save world
					converter().verbose("More than "+converter().targetChunksToKeepInRam+" chunks already in memory, giving them up to clean afterwards");
					
					//csWorld.saveEverything();
					//for(Region region : registeredCS_Regions)
					//	region.unregisterUser(user);

					for (ChunkHolder holder : registeredCS_Holders) {
						holder.unregisterUser(this);
						chunksAquired--;
					}

					for (Heightmap summary : registeredCS_Summaries)
						summary.unregisterUser(this);

					registeredCS_Summaries.clear();
					registeredCS_Holders.clear();

					//csWorld.unloadUselessData().traverse();
					converter().verbose("Done.");
				}
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

	public void dropAll() {
		CompoundFence readyAll = new CompoundFence();
		CompoundFence doneAll = new CompoundFence();
		SimpleFence atSignal = new SimpleFence();
		
		for(int i = 0; i < workers.length; i++) {
			SimpleFence ready = new SimpleFence();
			readyAll.add(ready);
			
			SimpleFence done = new SimpleFence();
			doneAll.add(done);
			
			scheduleTask(new Task() {

				@Override
				protected boolean task(TaskExecutor taskExecutor) {
					ready.signal();
					
					atSignal.traverse();
					
					ConverterWorkerThread cwt = (ConverterWorkerThread)taskExecutor;

					for (ChunkHolder holder : cwt.registeredCS_Holders) {
						holder.unregisterUser(cwt);
						cwt.chunksAquired--;
					}

					for (Heightmap summary : cwt.registeredCS_Summaries)
						summary.unregisterUser(cwt);

					cwt.registeredCS_Summaries.clear();
					cwt.registeredCS_Holders.clear();
					
					done.signal();
					
					return true;
				}
				
			});
		}
		
		readyAll.traverse();
		atSignal.signal();
		
		doneAll.traverse();
	}

	@Override
	public int submittedTasks() {
		return this.tasksQueueSize.get();
	}
}
