package io.xol.chunkstories.renderer.chunks;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.xol.chunkstories.api.util.concurrency.Fence;
import io.xol.chunkstories.api.workers.Task;
import io.xol.chunkstories.api.world.chunk.ChunkLightUpdater;
import io.xol.chunkstories.world.chunk.CubicChunk;
import io.xol.engine.concurrency.SimpleLock;

public class ClientChunkLightBaker implements ChunkLightUpdater {
	final CubicChunk chunk;
	
	final AtomicInteger unbakedUpdates = new AtomicInteger(1);
	public final SimpleLock onlyOneUpdateAtATime = new SimpleLock();
	
	protected TaskLightChunk task = null;
	final Lock taskLock = new ReentrantLock();
	
	public ClientChunkLightBaker(CubicChunk chunk) {
		this.chunk = chunk;
	}

	/** (non-Javadoc)
	 * @see io.xol.chunkstories.renderer.chunks.ChunkLightBaker#requestLightningUpdate()
	 */
	@Override
	public Fence requestLightningUpdate() {
		unbakedUpdates.incrementAndGet();
		
		Task fence;
		
		taskLock.lock();
		
		if(task == null || task.isDone() || task.isCancelled()) {
			task = new TaskLightChunk(chunk, true);
			chunk.getWorld().getGameContext().tasks().scheduleTask(task);
		}

		fence = task;
		
		taskLock.unlock();
		
		return fence;
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.renderer.chunks.ChunkLightBaker#spawnUpdateTaskIfNeeded()
	 */
	@Override
	public void spawnUpdateTaskIfNeeded() {
		if(unbakedUpdates.get() > 0) {
			taskLock.lock();
			
			if(task == null || task.isDone() || task.isCancelled()) {
				task = new TaskLightChunk(chunk, true);
				chunk.getWorld().getGameContext().tasks().scheduleTask(task);
			}
			
			taskLock.unlock();
		}
	}
	
	/* (non-Javadoc)
	 * @see io.xol.chunkstories.renderer.chunks.ChunkLightBaker#pendingUpdates()
	 */
	@Override
	public int pendingUpdates() {
		return this.unbakedUpdates.get();
	}
}
