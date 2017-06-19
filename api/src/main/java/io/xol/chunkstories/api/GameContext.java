package io.xol.chunkstories.api;

import io.xol.chunkstories.api.plugin.PluginManager;
import io.xol.chunkstories.api.util.ChunkStoriesLogger;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface GameContext
{
	public Content getContent();

	/** Accesses the pluginManager */
	public PluginManager getPluginManager();
	
	/** Prints some text, usefull for debug purposes */
	public void print(String message);

	/** Allows for writing to a .log file for debug purposes */
	public ChunkStoriesLogger logger();
}
