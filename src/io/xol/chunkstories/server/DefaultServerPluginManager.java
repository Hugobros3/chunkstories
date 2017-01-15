package io.xol.chunkstories.server;

import io.xol.chunkstories.api.plugin.ServerPluginManager;
import io.xol.chunkstories.api.server.ServerInterface;
import io.xol.chunkstories.plugin.DefaultPluginManager;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class DefaultServerPluginManager extends DefaultPluginManager implements ServerPluginManager
{
	ServerInterface server;
	
	public DefaultServerPluginManager(ServerInterface server)
	{
		super(server);
		this.server = server;
	}

	@Override
	public ServerInterface getServerInterface()
	{
		return server;
	}
}
