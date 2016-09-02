package io.xol.chunkstories.api.plugin.commands;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class Command
{
	private String name;
	private Set<String> aliases = new HashSet<String>();

	public Command(String name)
	{
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
