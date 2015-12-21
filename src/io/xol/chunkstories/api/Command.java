package io.xol.chunkstories.api;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class Command
{
	public String name;

	public String getName()
	{
		return name;
	}

	public Command(String name)
	{
		this.name = name;
	}

	public boolean equals(Object o)
	{
		return ((Command) o).name.equals(name);
	}

	public int hashCode()
	{
		return name.hashCode();
	}
}
