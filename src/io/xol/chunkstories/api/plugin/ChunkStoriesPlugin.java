package io.xol.chunkstories.api.plugin;

import io.xol.chunkstories.api.plugin.client.ClientInterface;
import io.xol.chunkstories.api.plugin.server.Command;
import io.xol.chunkstories.api.plugin.server.ServerInterface;
import io.xol.chunkstories.server.tech.CommandEmitter;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class ChunkStoriesPlugin
{
	protected ServerInterface serverI;
	protected ClientInterface clientI;
	
	protected PluginManager pluginManager;
	
	public void setServer(ServerInterface server)
	{
		this.serverI = server;
	}
	
	public ServerInterface getServer()
	{
		return serverI;
	}
	
	public PluginManager getPluginsManager()
	{
		return pluginManager;
	}
	
	public boolean handleCommand(CommandEmitter sender, Command cmd, String[] a, String rawText)
	{
		System.out.println("Someone left the default command handler !");
		return false;
	}
	
	public abstract void onEnable();
	public abstract void onDisable();

	public void setPluginManager(PluginManager pluginManager)
	{
		this.pluginManager = pluginManager;
	}
}
