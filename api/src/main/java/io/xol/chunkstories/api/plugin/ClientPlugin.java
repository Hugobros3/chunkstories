package io.xol.chunkstories.api.plugin;

import io.xol.chunkstories.api.client.ClientInterface;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** A type of plugin that exclusivly runs on the client */
public abstract class ClientPlugin extends ChunkStoriesPlugin
{
	private final ClientInterface clientInterface;

	public ClientPlugin(PluginInformation pluginInformation, ClientInterface clientInterface)
	{
		super(pluginInformation, clientInterface);
		this.clientInterface = clientInterface;
	}

	public ClientInterface getClientInterface()
	{
		return clientInterface;
	}
}
