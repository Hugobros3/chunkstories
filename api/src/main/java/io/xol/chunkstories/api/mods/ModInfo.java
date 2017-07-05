package io.xol.chunkstories.api.mods;

import io.xol.chunkstories.api.content.NamedWithProperties;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Loads from mod.txt in the mod root directory
 */
public interface ModInfo extends NamedWithProperties
{
	public Mod getMod();

	/** Get unique mod name */
	public String getInternalName();
	
	/** Get human-readable mod name */
	public String getName();

	public String getVersion();

	public String getDescription();
}