package io.xol.chunkstories.api.mods;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

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
	 * Accesses the asset data.
	 */
	public default Reader reader()
	{
		return new InputStreamReader(read());
	}
	
	/**
	 * Returns the mod this asset originated from
	 */
	public Mod getSource();
}
