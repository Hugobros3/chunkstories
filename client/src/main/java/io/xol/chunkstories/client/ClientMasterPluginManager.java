//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.client;

import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.plugin.ClientPluginManager;
import io.xol.chunkstories.api.plugin.ServerPluginManager;
import io.xol.chunkstories.api.server.ServerInterface;
import io.xol.chunkstories.plugin.DefaultPluginManager;
import io.xol.chunkstories.server.LocalServerContext;

public class ClientMasterPluginManager extends DefaultPluginManager implements ClientPluginManager, ServerPluginManager
{
	LocalServerContext localServerContext;
	
	public ClientMasterPluginManager(LocalServerContext localServerContext)
	{
		super(localServerContext);
		this.localServerContext = localServerContext;
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
