//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.client;

import io.xol.chunkstories.api.client.IngameClient;
import io.xol.chunkstories.api.plugin.ClientPluginManager;
import io.xol.chunkstories.plugin.DefaultPluginManager;

public class ClientSlavePluginManager extends DefaultPluginManager implements ClientPluginManager {
	IngameClient client;

	public ClientSlavePluginManager(IngameClient client) {
		super(client);
		this.client = client;

		this.reloadPlugins();
	}

	@Override
	public IngameClient getClient() {
		return client;
	}

}
