package io.xol.chunkstories.api.plugin;

import io.xol.chunkstories.api.server.ServerInterface;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface ServerPluginManager extends PluginManager
{
	public ServerInterface getServerInterface();
}
