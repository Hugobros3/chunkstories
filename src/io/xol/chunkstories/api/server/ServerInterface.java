package io.xol.chunkstories.api.server;

import java.util.Iterator;

import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.plugin.PluginManager;
import io.xol.chunkstories.api.plugin.context.PluginExecutionContext;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface ServerInterface extends GameContext, PluginExecutionContext
{
	public Iterator<Player> getConnectedPlayers();

	public Player getPlayer(String string);
	
	public Player getPlayerByUUID(long UUID);

	public PluginManager getPluginManager();

	public void broadcastMessage(String message);
}
