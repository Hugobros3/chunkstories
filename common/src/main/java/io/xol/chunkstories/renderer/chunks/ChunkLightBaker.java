package io.xol.chunkstories.renderer.chunks;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.xol.chunkstories.world.chunk.CubicChunk;
import io.xol.engine.concurrency.SimpleLock;

public class ChunkLightBaker {
	final CubicChunk chunk;
	
	final AtomicInteger unbakedUpdates = new AtomicInteger(1);
	public final SimpleLock onlyOneLightUpdateAtATime = new SimpleLock();
	
	protected TaskLightChunk task = null;
	final ReadWriteLock taskLock = new ReentrantReadWriteLock();
	
	public ChunkLightBaker(CubicChunk chunk) {
		this.chunk = chunk;
	}

	/** Increments the needed updates counter, spawns a task if none exists or is pending execution */
	public void requestLightningUpdate() {
		unbakedUpdates.incrementAndGet();
		
		taskLock.writeLock().lock();
		
		if(task == null || task.isDone() || task.isCancelled()) {
			task = new TaskLightChunk(chunk, true);
			chunk.getWorld().getGameContext().tasks().scheduleTask(task);
		}
		
		taskLock.writeLock().unlock();
	}

	/** Spawns a TaskLightChunk if there are unbaked modifications and no task is pending execution */
	public void checkIfUpdatedIsNeeded() {
		if(unbakedUpdates.get() > 0) {
			taskLock.writeLock().lock();
			
			if(task == null || task.isDone() || task.isCancelled()) {
				task = new TaskLightChunk(chunk, true);
				chunk.getWorld().getGameContext().tasks().scheduleTask(task);
			}
			
			taskLock.writeLock().unlock();
		}
	}
	
	/** Returns how many light updates have yet to be done */
	public int pendingUpdates() {
		return this.unbakedUpdates.get();
	}
}
