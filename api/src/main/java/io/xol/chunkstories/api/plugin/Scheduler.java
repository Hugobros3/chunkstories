package io.xol.chunkstories.api.plugin;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** Quick and dirty interface to reproduce Bukkit scheduler functionality for the time being */
public interface Scheduler
{
	public void scheduleSyncRepeatingTask(ChunkStoriesPlugin p, Runnable runnable, long l, long m);
}
