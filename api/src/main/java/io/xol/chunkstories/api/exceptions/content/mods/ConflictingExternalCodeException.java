package io.xol.chunkstories.api.exceptions.content.mods;

import io.xol.chunkstories.api.mods.Mod;

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
