package io.xol.chunkstories.api.plugin;

import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.plugin.commands.Command;
import io.xol.chunkstories.api.plugin.commands.CommandEmitter;
import io.xol.chunkstories.api.plugin.commands.CommandHandler;
import io.xol.chunkstories.api.server.ServerInterface;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class ChunkStoriesPlugin implements CommandHandler
{
	protected ServerInterface serverI;
	protected ClientInterface clientI;
	
	protected PluginManager pluginManager;
	public PluginJar jar;
	
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
	
	public boolean handleCommand(CommandEmitter sender, Command cmd, String[] arguments)
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

	public String getName()
	{
		if(jar == null)
			return "you were adopted";
		return jar.getName();
	}
}
