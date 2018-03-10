//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.chunk;

import io.xol.chunkstories.api.rendering.world.chunk.ChunkRenderable;
import io.xol.chunkstories.api.workers.Task;
import io.xol.chunkstories.api.workers.TaskExecutor;

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
			chunk.lightBaker.onlyOneUpdateAtATime.lock();
			int updatesNeeded = chunk.lightBaker.unbakedUpdates.get();
			if(updatesNeeded == 0)
				return true;
			
			//Actual computation takes place here
			int mods = chunk.lightBaker.computeVoxelLightningInternal(updateAdjacentChunks);
			
			//Blocks have changed ?
			if(mods > 0 && chunk instanceof ChunkRenderable)
				((ChunkRenderable)chunk).meshUpdater().requestMeshUpdate();
			
			//Remove however many updates were pending
			chunk.lightBaker.unbakedUpdates.addAndGet(-updatesNeeded);
		}
		finally {
			//chunk.lightBakingStatus.taskLock.writeLock().lock();
			//chunk.lightBakingStatus.task = null;
			//chunk.lightBakingStatus.taskLock.writeLock().unlock();
			
			chunk.lightBaker.onlyOneUpdateAtATime.unlock();
		}
		return true;
	}

}
