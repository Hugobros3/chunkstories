package io.xol.chunkstories.api;

import java.util.Set;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface ServerInterface
{
	public Set<Player> getConnectedPlayers();

	public PluginManager getPluginsManager();
}
