package io.xol.chunkstories.api.plugin;

import io.xol.chunkstories.api.server.ServerInterface;
import io.xol.chunkstories.content.DefaultPluginManager;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ServerPluginManager extends DefaultPluginManager
{
	ServerInterface ServerInterface;
	
	public ServerPluginManager(ServerInterface ServerInterface)
	{
		super(ServerInterface);
		this.ServerInterface = ServerInterface;
	}

}
