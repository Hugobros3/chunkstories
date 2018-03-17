//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.chunk;

import io.xol.chunkstories.api.rendering.world.chunk.ChunkRenderable;
import io.xol.chunkstories.api.workers.Task;
import io.xol.chunkstories.api.workers.TaskExecutor;
import io.xol.chunkstories.world.WorldImplementation;

public class TaskLightChunk extends Task {

	final WorldImplementation world;
	final CubicChunk chunk;
	final int chunkX, chunkY, chunkZ;
	final CubicChunk leftChunk, rightChunk, topChunk, bottomChunk, frontChunk, backChunk;
	
	final boolean updateAdjacentChunks;
	
	public TaskLightChunk(CubicChunk chunk, boolean updateAdjacentChunks) {
		this.chunk = chunk;
		this.world = chunk.world;
		this.updateAdjacentChunks = updateAdjacentChunks;

		this.chunkX = chunk.chunkX;
		this.chunkY = chunk.chunkY;
		this.chunkZ = chunk.chunkZ;
		
		// Checks if the adjacent chunks are done loading
		topChunk = world.getChunk(chunkX, chunkY + 1, chunkZ);
		bottomChunk = world.getChunk(chunkX, chunkY - 1, chunkZ);
		frontChunk = world.getChunk(chunkX, chunkY, chunkZ + 1);
		backChunk = world.getChunk(chunkX, chunkY, chunkZ - 1);
		leftChunk = world.getChunk(chunkX - 1, chunkY, chunkZ);
		rightChunk = world.getChunk(chunkX + 1, chunkY, chunkZ);
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
			
			return true;
		}
		finally {
			chunk.lightBaker.taskLock.lock();
			
			//Re-schedule a new task immediately if updates happened while we worked
			if(chunk.lightBaker.unbakedUpdates.get() > 0) {
				chunk.lightBaker.task = new TaskLightChunk(chunk, true);
				chunk.getWorld().getGameContext().tasks().scheduleTask(chunk.lightBaker.task);
			}
			//Set the task reference to null so a new task can be spawned as needed
			else {
				chunk.lightBaker.task = null;
			}
			chunk.lightBaker.taskLock.unlock();
			
			//Unlock that
			chunk.lightBaker.onlyOneUpdateAtATime.unlock();
		}
	}

}
