package io.xol.chunkstories.api.plugin;

public interface Scheduler
{
	public void scheduleSyncRepeatingTask(ChunkStoriesPlugin p, Runnable runnable, long l, long m);

}
