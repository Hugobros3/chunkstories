package io.xol.chunkstories.renderer.chunks;

import io.xol.chunkstories.api.workers.Task;
import io.xol.chunkstories.api.workers.TaskExecutor;
import io.xol.chunkstories.world.chunk.CubicChunk;

public class TaskLightChunk extends Task {

	final CubicChunk chunk;
	final boolean updateAdjacentChunks;
	
	public TaskLightChunk(CubicChunk chunk, boolean updateAdjacentChunks) {
		this.chunk = chunk;
		this.updateAdjacentChunks = updateAdjacentChunks;
	}

	@Override
	protected boolean task(TaskExecutor taskExecutor) {
		
		try {
			//Lock this
			chunk.lightBakingStatus.onlyOneUpdateAtATime.lock();
			int updatesNeeded = chunk.lightBakingStatus.unbakedUpdates.get();
			if(updatesNeeded == 0)
				return true;
			
			//Actual computation takes place here
			chunk.computeVoxelLightningInternal(updateAdjacentChunks);
			
			//Remove however many updates were pending
			chunk.lightBakingStatus.unbakedUpdates.addAndGet(-updatesNeeded);
		}
		finally {
			//chunk.lightBakingStatus.taskLock.writeLock().lock();
			//chunk.lightBakingStatus.task = null;
			//chunk.lightBakingStatus.taskLock.writeLock().unlock();
			
			chunk.lightBakingStatus.onlyOneUpdateAtATime.unlock();
		}
		return true;
	}

}
