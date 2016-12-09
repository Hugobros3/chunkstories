package io.xol.chunkstories.api.plugin;

import io.xol.chunkstories.api.plugin.commands.Command;
import io.xol.chunkstories.api.plugin.commands.CommandEmitter;
import io.xol.chunkstories.api.plugin.commands.CommandHandler;
import io.xol.chunkstories.api.plugin.context.PluginExecutionContext;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class ChunkStoriesPlugin implements CommandHandler
{
	protected final PluginExecutionContext pluginExecutionContext;
	
	private final PluginInformation pluginInformation;
	
	public ChunkStoriesPlugin(PluginInformation pluginInformation, PluginExecutionContext pluginExecutionContext)
	{
		this.pluginInformation = pluginInformation;
		this.pluginExecutionContext = pluginExecutionContext;
	}
	
	public PluginInformation getPluginInformation()
	{
		return pluginInformation;
	}
	
	public PluginExecutionContext getPluginExecutionContext()
	{
		return pluginExecutionContext;
	}
	
	public PluginManager getPluginManager()
	{
		return pluginExecutionContext.getPluginManager();
	}
	
	public boolean handleCommand(CommandEmitter sender, Command cmd, String[] arguments)
	{
		System.out.println("Someone left the default command handler !");
		return false;
	}
	
	public abstract void onEnable();
	public abstract void onDisable();

	public String getName()
	{
		return pluginInformation.getName();
	}
}
