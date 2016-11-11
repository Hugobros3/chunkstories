package io.xol.chunkstories.world.io;

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

	@Override
	public IOTask requestChunkLoad(ChunkHolderImplementation r)
	{
		IOTaskLoadChunk task = new IOTaskLoadChunk(r);
		if(tasks.add(task))
		{
			//System.out.println("Added task ioloadchunk"+chunkX+":"+chunkY+":"+chunkZ);
			task.run();
			tasks.remove(task);
		}
		return task;
	}

	@Override
	public void requestRegionLoad(RegionImplementation region)
	{
		IOTask task = new IOTaskLoadRegion(region);
		if(tasks.add(task))
		{
			task.run();
			tasks.remove(task);
		}
	}

	@Override
	public void requestRegionSave(RegionImplementation holder)
	{
		IOTask task = new IOTaskSaveRegion(holder);
		if(tasks.add(task))
		{
			task.run();
			tasks.remove(task);
		}
	}

	@Override
	public void requestRegionSummaryLoad(RegionSummaryImplementation summary)
	{
		IOTask task = new IOTaskLoadSummary(summary);
		if(tasks.add(task))
		{
			task.run();
			tasks.remove(task);
		}
	}

	@Override
	public void requestRegionSummarySave(RegionSummaryImplementation summary)
	{
		IOTask task = new IOTaskSaveSummary(summary);
		if(tasks.add(task))
		{
			task.run();
			tasks.remove(task);
		}
	}
}
