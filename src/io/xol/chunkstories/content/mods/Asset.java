package io.xol.chunkstories.content.mods;

import java.io.InputStream;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Assets are used as files in the game's virtual file system
 */
public interface Asset
{
	/**
	 * Returns the standardized name of the asset, started by ./
	 * Examples : 
	 * ./voxels/textures/air.png
	 * ./sounds/footsteps/jump.ogg
	 * ./shaders/weather/weather.fs
	 */
	public String getName();
	
	/**
	 * Accesses the asset data.
	 */
	public InputStream read();
	
	/**
	 * Returns the mod this asset originated from
	 */
	public Mod getSource();
}
