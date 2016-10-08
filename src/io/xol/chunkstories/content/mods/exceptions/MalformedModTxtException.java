package io.xol.chunkstories.content.mods.exceptions;

import io.xol.chunkstories.content.mods.ModInfo;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class MalformedModTxtException extends ModLoadFailureException
{
	public MalformedModTxtException(ModInfo ok)
	{
		super(null, "Malformed txt info file or  missing one.");
	}

	private static final long serialVersionUID = 9218958020915706786L;

}
