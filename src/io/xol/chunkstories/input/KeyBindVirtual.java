package io.xol.chunkstories.input;

import io.xol.chunkstories.api.input.KeyBind;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class KeyBindVirtual implements KeyBind
{
	String name;

	public KeyBindVirtual(String name)
	{
		this.name = name;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public boolean isPressed()
	{
		return true;
	}

	public String toString()
	{
		return "[KeyBindVirtual: " + name + "]";
	}
}
