package io.xol.chunkstories.api.mods;

import io.xol.chunkstories.api.util.IterableIterator;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * A mod contains assets that add to or override the game's defaults.
 */
public interface Mod
{
	/**
	 * Returns the asset corresponding to the provided path, matching the syntax ./directory/subdirectory/asset.txt
	 * Returns only the version defined in this mod.
	 * Returns null if the asset couln't be found
	 */
	public Asset getAssetByName(String name);

	/** Iterates over this mod's assets */
	public IterableIterator<Asset> assets();

	public ModInfo getModInfo();

	public String getMD5Hash();

}