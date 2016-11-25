package io.xol.chunkstories.world.io;

import java.util.Iterator;

import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.chunk.ChunkHolderImplementation;
import io.xol.chunkstories.world.region.RegionImplementation;
import io.xol.chunkstories.world.summary.RegionSummaryImplementation;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class IOTasksImmediate extends IOTasks {

	public IOTasksImmediate(WorldImplementation world) {
		super(world);
		//this.tasks = null;
	}
	
	private void runTask(IOTask task)
	{
		if(tasks.size() > 50)
		{
			System.out.println("IOTask size > 50 ! Dumping then crashing");
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
			task.run();
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
	public void requestRegionSave(RegionImplementation holder)
	{
		IOTask task = new IOTaskSaveRegion(holder);
		runTask(task);
	}

	@Override
	public void requestRegionSummaryLoad(RegionSummaryImplementation summary)
	{
		IOTask task = new IOTaskLoadSummary(summary);
		runTask(task);
	}

	@Override
	public void requestRegionSummarySave(RegionSummaryImplementation summary)
	{
		IOTask task = new IOTaskSaveSummary(summary);
		runTask(task);
	}
}
