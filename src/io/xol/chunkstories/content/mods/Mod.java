package io.xol.chunkstories.content.mods;

import io.xol.chunkstories.api.utils.IterableIterator;
import io.xol.chunkstories.content.mods.exceptions.ModLoadFailureException;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class Mod
{
	protected ModInfo modInfo;
	
	Mod() throws ModLoadFailureException
	{
		
	}
	
	public abstract Asset getAssetByName(String name);
	
	public abstract IterableIterator<Asset> assets();
	
	public ModInfo getModInfo()
	{
		return modInfo;
	}
	
	public abstract String getMD5Hash();
	
	public abstract void close();

	public abstract String getLoadString();
}
