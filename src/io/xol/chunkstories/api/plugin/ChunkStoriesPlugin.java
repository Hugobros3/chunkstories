package io.xol.chunkstories.api.plugin;

import io.xol.chunkstories.api.GameContext;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class ChunkStoriesPlugin
{
	protected final GameContext pluginExecutionContext;
	
	private final PluginInformation pluginInformation;
	
	public ChunkStoriesPlugin(PluginInformation pluginInformation, GameContext pluginExecutionContext)
	{
		this.pluginInformation = pluginInformation;
		this.pluginExecutionContext = pluginExecutionContext;
	}
	
	public PluginInformation getPluginInformation()
	{
		return pluginInformation;
	}
	
	public GameContext getPluginExecutionContext()
	{
		return pluginExecutionContext;
	}
	
	public PluginManager getPluginManager()
	{
		return pluginExecutionContext.getPluginManager();
	}
	
	public abstract void onEnable();
	public abstract void onDisable();

	public String getName()
	{
		return pluginInformation.getName();
	}
	
	/*public File getPluginFolder()
	{
		return new File(GameDirectory.getGameFolderPath()+"/plugins/"+pluginInformation.g);
	}*/
}
