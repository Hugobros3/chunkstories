package io.xol.chunkstories.api.server;

import java.util.Iterator;
import io.xol.chunkstories.api.plugin.PluginManager;
import io.xol.chunkstories.api.plugin.commands.CommandEmitter;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface ServerInterface extends CommandEmitter
{
	public Iterator<Player> getConnectedPlayers();

	public Player getPlayer(String string);
	
	public Player getPlayerByUUID(long UUID);

	public PluginManager getPluginsManager();
}
