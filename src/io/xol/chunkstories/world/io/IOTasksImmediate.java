package io.xol.chunkstories.world.io;

import io.xol.chunkstories.world.World;
import io.xol.chunkstories.world.chunk.ChunkHolder;
import io.xol.chunkstories.world.summary.ChunkSummary;

public class IOTasksImmediate extends IOTasks {

	public IOTasksImmediate(World world) {
		super(world);
		this.tasks = null;
	}

	@Override
	public void requestChunkLoad(int chunkX, int chunkY, int chunkZ, boolean overwrite)
	{
		IOTaskLoadChunk task = new IOTaskLoadChunk(chunkX, chunkY, chunkZ, true, overwrite);
		/*synchronized (tasks)
		{
			for (IOTask ioTask : tasks)
			{
				if (ioTask instanceof IOTaskLoadChunk)
				{
					IOTaskLoadChunk taskLC = (IOTaskLoadChunk) ioTask;
					if (taskLC.x == task.x && taskLC.y == task.y && taskLC.z == task.z)
						return;
				}
			}
		}*/
		task.run();
	}

	@Override
	public void requestChunkHolderLoad(ChunkHolder holder)
	{
		IOTask task = new IOTaskLoadChunkHolder(holder);
		task.run();
	}

	@Override
	public void requestChunkHolderSave(ChunkHolder holder)
	{
		IOTask task = new IOTaskSaveChunkHolder(holder);
		//System.out.println("bs");
		task.run();
	}

	@Override
	public void requestChunkSummaryLoad(ChunkSummary summary)
	{
		IOTask task = new IOTaskLoadSummary(summary);
		task.run();
	}

	@Override
	public void requestChunkSummarySave(ChunkSummary summary)
	{
		IOTask task = new IOTaskSaveSummary(summary);
		task.run();
	}
}
