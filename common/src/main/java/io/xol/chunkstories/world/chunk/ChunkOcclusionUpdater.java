//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.chunk;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.xol.chunkstories.api.util.concurrency.Fence;
import io.xol.chunkstories.api.workers.Task;
import io.xol.engine.concurrency.SimpleLock;

public class ChunkOcclusionUpdater {

	final CubicChunk chunk;
	
	final AtomicInteger unbakedUpdates = new AtomicInteger(1);
	
	protected TaskComputeChunkOcclusion task = null;
	final Lock taskLock = new ReentrantLock();
	
	// Occlusion lookup, there are 6 sides you can enter a chunk by and 5 sides you can exit it by. we use 6 coz it's easier and who the fuck cares about a six-heights of a byte
	public final SimpleLock onlyOneUpdateAtATime = new SimpleLock();
	public boolean occlusionSides[][];
	
	public ChunkOcclusionUpdater(CubicChunk chunk) {
		this.chunk = chunk;
	}
	
	public Fence requestUpdate() {
		unbakedUpdates.incrementAndGet();
		
		Task fence;
		
		taskLock.lock();
		
		if(task == null || task.isDone() || task.isCancelled()) {
			task = new TaskComputeChunkOcclusion(chunk);
			chunk.getWorld().getGameContext().tasks().scheduleTask(task);
		}

		fence = task;
		
		taskLock.unlock();
		
		return fence;
	}

	public void spawnUpdateTaskIfNeeded() {
		if(unbakedUpdates.get() > 0) {
			taskLock.lock();
			
			if(task == null || task.isDone() || task.isCancelled()) {
				task = new TaskComputeChunkOcclusion(chunk);
				chunk.getWorld().getGameContext().tasks().scheduleTask(task);
			}
			
			taskLock.unlock();
		}
	}
	
	public int pendingUpdates() {
		return this.unbakedUpdates.get();
	}

	public void destroy() {
		Task task = this.task;
		if(task != null)
			task.cancel();
	}
}
