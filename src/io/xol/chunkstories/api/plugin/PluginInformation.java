package io.xol.chunkstories.api.plugin;

import java.util.Collection;

import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.exceptions.plugins.PluginCreationException;
import io.xol.chunkstories.api.plugin.commands.Command;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface PluginInformation
{
	public String getName();

	public String getPluginVersion();

	public String getAuthor();

	public PluginType getPluginType();

	public ChunkStoriesPlugin createInstance(GameContext pluginExecutionContext) throws PluginCreationException;

	public Collection<Command> getCommands();
	
	public enum PluginType
	{
		UNIVERSAL, CLIENT_ONLY, MASTER;
	}
}