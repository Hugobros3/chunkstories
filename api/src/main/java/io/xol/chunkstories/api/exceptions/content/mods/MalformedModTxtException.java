package io.xol.chunkstories.api.exceptions.content.mods;

import io.xol.chunkstories.api.mods.ModInfo;

//(c) 2015-2017 XolioWare Interactive
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
