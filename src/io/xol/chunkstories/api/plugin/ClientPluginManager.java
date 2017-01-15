package io.xol.chunkstories.api.plugin;

import io.xol.chunkstories.api.client.ClientInterface;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface ClientPluginManager extends PluginManager
{
	public ClientInterface getClientInterface();
}
