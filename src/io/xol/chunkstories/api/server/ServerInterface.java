package io.xol.chunkstories.api.server;

import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.plugin.PluginManager;
import io.xol.chunkstories.api.utils.IterableIterator;
import io.xol.chunkstories.api.world.World;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface ServerInterface extends GameContext
{
	public IterableIterator<Player> getConnectedPlayers();

	public Player getPlayerByName(String string);
	
	public Player getPlayerByUUID(long UUID);

	public PluginManager getPluginManager();

	public void broadcastMessage(String message);

	public World getWorld();
}
