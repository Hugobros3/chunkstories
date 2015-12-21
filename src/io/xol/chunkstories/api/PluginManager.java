package io.xol.chunkstories.api;

import io.xol.chunkstories.api.events.Listener;
import io.xol.chunkstories.server.tech.CommandEmitter;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface PluginManager
{
	public void disablePlugins();
	public void reloadPlugins();

	public boolean dispatchCommand(String cmd, CommandEmitter emitter);
	public void registerEventListener(Listener l, JavaPlugin plugin);
}
