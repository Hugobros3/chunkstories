package io.xol.chunkstories.api.server;

import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.plugin.PluginManager;
import io.xol.chunkstories.api.util.IterableIterator;
import io.xol.chunkstories.api.world.WorldMaster;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface ServerInterface extends GameContext
{
	public IterableIterator<Player> getConnectedPlayers();

	public Player getPlayerByName(String string);
	
	public Player getPlayerByUUID(long UUID);

	public PluginManager getPluginManager();

	public PermissionsManager getPermissionsManager();
	
	public void installPermissionsManager(PermissionsManager permissionsManager);
	
	public void broadcastMessage(String message);

	public WorldMaster getWorld();
}
