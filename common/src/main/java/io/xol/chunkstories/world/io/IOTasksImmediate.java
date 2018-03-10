//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.io;

import java.util.Iterator;

import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.chunk.ChunkHolderImplementation;
import io.xol.chunkstories.world.region.RegionImplementation;
import io.xol.chunkstories.world.summary.RegionSummaryImplementation;

public class IOTasksImmediate extends IOTasks {

	public IOTasksImmediate(WorldImplementation world) {
		super(world);
		//this.tasks = null;
	}
	
	private void runTask(IOTask task)
	{
		if(tasks.size() > 50)
		{
			//Shouldn't happen, this indicates the tasks keep being submitted in another task and not fullfilling.
			System.out.println("Immediate IOTask size > 50 ! Dumping then crashing");
			Iterator<IOTask> i = tasks.iterator();
			while(i.hasNext())
			{
				System.out.println(i.next());
			}
			Thread.dumpStack();
			Runtime.getRuntime().exit(-1);
		}
		if(tasks.add(task))
		{
			task.run(this);
			tasks.remove(task);
		}
	}

	@Override
	public IOTask requestChunkLoad(ChunkHolderImplementation r)
	{
		IOTaskLoadChunk task = new IOTaskLoadChunk(r);
		runTask(task);
		return task;
	}

	@Override
	public void requestRegionLoad(RegionImplementation region)
	{
		IOTask task = new IOTaskLoadRegion(region);
		runTask(task);
	}

	@Override
	public IOTask requestRegionSave(RegionImplementation holder)
	{
		IOTask task = new IOTaskSaveRegion(holder);
		runTask(task);
		
		//Already completed ...
		return task;
	}

	@Override
	public IOTaskLoadSummary requestRegionSummaryLoad(RegionSummaryImplementation summary)
	{
		IOTaskLoadSummary task = new IOTaskLoadSummary(summary);
		runTask(task);
		
		return task;
	}

	@Override
	public IOTaskSaveSummary requestRegionSummarySave(RegionSummaryImplementation summary)
	{
		IOTaskSaveSummary task = new IOTaskSaveSummary(summary);
		runTask(task);
		
		return task;
	}
}
