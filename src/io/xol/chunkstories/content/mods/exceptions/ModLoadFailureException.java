package io.xol.chunkstories.content.mods.exceptions;

import io.xol.chunkstories.api.mods.Mod;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ModLoadFailureException extends Exception
{
	private static final long serialVersionUID = -3181028531069214061L;
	
	Mod mod;
	String message;
	
	public ModLoadFailureException(Mod mod, String message)
	{
		this.mod = mod;
		this.message = message;
	}

	public String getMessage()
	{
		return "Mod '"+mod+"' failed to load : "+message;
	}
}
