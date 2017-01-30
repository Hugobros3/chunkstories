package io.xol.chunkstories.client;

import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.plugin.ClientPluginManager;
import io.xol.chunkstories.plugin.DefaultPluginManager;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ClientSlavePluginManager extends DefaultPluginManager implements ClientPluginManager
{
	ClientInterface client;
	
	public ClientSlavePluginManager(ClientInterface client)
	{
		super(client);
		this.client = client;
	}

	@Override
	public ClientInterface getClientInterface()
	{
		return client;
	}

}
