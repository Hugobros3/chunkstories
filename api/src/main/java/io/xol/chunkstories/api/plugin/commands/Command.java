package io.xol.chunkstories.api.plugin.commands;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import io.xol.chunkstories.api.plugin.PluginInformation;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class Command
{
	private PluginInformation plugin;
	
	private final String name;
	private Set<String> aliases = new HashSet<String>();
	
	private CommandHandler handler = null;

	public Command(PluginInformation plugin, String name)
	{
		this.plugin = plugin;
		
		this.name = name;
		this.aliases.add(name);
	}
	
	public void addAlias(String alias)
	{
		this.aliases.add(alias);
	}
	
	public String getName()
	{
		return name;
	}
	
	public PluginInformation getPlugin()
	{
		return plugin;
	}

	public void setHandler(CommandHandler commandHandler)
	{
		this.handler = commandHandler;
	}
	
	public CommandHandler getHandler()
	{
		return this.handler;
	}

	@Override
	public boolean equals(Object o)
	{
		if(o instanceof String)
			return ((String)o).equals(name);
		return ((Command) o).name.equals(name);
	}

	@Override
	public int hashCode()
	{
		return name.hashCode();
	}

	public Collection<String> aliases()
	{
		return aliases;
	}
}
