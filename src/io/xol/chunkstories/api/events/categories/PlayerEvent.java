package io.xol.chunkstories.api.events.categories;

import io.xol.chunkstories.api.plugin.server.Player;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Describes an event triggered, centered arround or related to a player.
 * @author Hugo
 *
 */
public interface PlayerEvent
{
	/**
	 * Returns the player affected by the event. If two or more players are concerned, only the 'main' one will be returned.
	 * @return
	 */
	public Player getPlayer();
}
