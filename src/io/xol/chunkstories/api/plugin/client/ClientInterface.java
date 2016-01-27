package io.xol.chunkstories.api.plugin.client;

import io.xol.chunkstories.api.sound.SoundManager;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface ClientInterface
{
	/**
	 * Returns the {@link SoundManager} in use
	 * @return
	 */
	public SoundManager getSoundManager();
	
	/**
	 * Reloads all assets, shaders, sounds and whatnot from the mods and the main game.
	 */
	public void reloadAssets();
}
