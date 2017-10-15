package io.xol.chunkstories.client;

import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.plugin.ClientPluginManager;
import io.xol.chunkstories.api.plugin.ServerPluginManager;
import io.xol.chunkstories.api.server.ServerInterface;
import io.xol.chunkstories.plugin.DefaultPluginManager;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ClientMasterPluginManager extends DefaultPluginManager implements ClientPluginManager, ServerPluginManager
{
	LocalServerContext localServerContext;
	
	public ClientMasterPluginManager(LocalServerContext localServerContext)
	{
		super(localServerContext);
		this.localServerContext = localServerContext;
		this.reloadPlugins();
	}

	@Override
	public ClientInterface getClientInterface()
	{
		return localServerContext;
	}

	@Override
	public ServerInterface getServerInterface()
	{
		return localServerContext;
	}

}
