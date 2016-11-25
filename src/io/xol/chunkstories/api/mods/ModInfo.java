package io.xol.chunkstories.api.mods;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Pretty straightforward, isn't it ?
 */
public interface ModInfo
{
	public Mod getMod();
	
	public String getName();

	public String getVersion();

	public String getDescription();
}