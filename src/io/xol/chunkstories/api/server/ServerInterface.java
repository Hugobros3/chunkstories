package io.xol.chunkstories.api.server;

import java.util.Set;

import io.xol.chunkstories.api.plugin.PluginManager;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface ServerInterface
{
	public Set<Player> getConnectedPlayers();

	public Player getPlayer(String string);

	public PluginManager getPluginsManager();
}
