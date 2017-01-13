package io.xol.chunkstories.api.plugin;

import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.content.DefaultPluginManager;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ClientPluginManager extends DefaultPluginManager
{
	ClientInterface clientInterface;
	
	public ClientPluginManager(ClientInterface clientInterface)
	{
		super(clientInterface);
		this.clientInterface = clientInterface;
	}
}
