//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.client;

import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.plugin.ClientPluginManager;
import io.xol.chunkstories.plugin.DefaultPluginManager;

public class ClientSlavePluginManager extends DefaultPluginManager implements ClientPluginManager {
	ClientInterface client;

	public ClientSlavePluginManager(Client client) {
		super(client);
		this.client = client;

		client.setClientPluginManager(this);
		this.reloadPlugins();
	}

	@Override
	public ClientInterface getClientInterface() {
		return client;
	}

}
