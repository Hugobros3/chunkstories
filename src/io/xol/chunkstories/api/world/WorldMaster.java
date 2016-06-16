package io.xol.chunkstories.api.world;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.components.Subscriber;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * A 'master' world is one hosting the game logic and who runs the 'serverside' plugins. It can be either a dedicated server or a singleplayer world.
 */
public interface WorldMaster
{
	public void playSoundEffectExcluding(String soundEffect, Location location, float pitch, float gain, Subscriber subscriber);
}
