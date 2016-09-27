package io.xol.chunkstories.content.mods.exceptions;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

import io.xol.chunkstories.content.mods.Mod;

public class ConflictingExternalCodeException extends ModLoadFailureException
{

	/**
	 * 
	 */
	private static final long serialVersionUID = -6933179574010715068L;

	public ConflictingExternalCodeException(Mod mod, String message)
	{
		super(mod, message);
	}

}
