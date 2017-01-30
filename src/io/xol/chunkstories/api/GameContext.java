package io.xol.chunkstories.api;

import io.xol.chunkstories.api.plugin.PluginManager;

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
}
