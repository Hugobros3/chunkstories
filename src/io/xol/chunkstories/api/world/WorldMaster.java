package io.xol.chunkstories.api.world;

import io.xol.chunkstories.api.server.Player;
import io.xol.chunkstories.api.utils.IterableIterator;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * A 'master' world is one hosting the game logic and who runs the 'serverside' plugins. It can be either a dedicated server or a singleplayer world.
 */
public interface WorldMaster extends World
{
	public void spawnPlayer(Player player);
	
	public IterableIterator<Player> getPlayers();
	
	/**
	 * Plays a soundEffect to all clients except once, typical use if sounds played locally by a player that can't suffer any lag for him
	 * but still need others to hear it as well
	 */
	//public void playSoundEffectExcluding(String soundEffect, Location location, float pitch, float gain, Subscriber subscriber);
}
