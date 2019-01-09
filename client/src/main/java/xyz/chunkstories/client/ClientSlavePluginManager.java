//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.client;

import xyz.chunkstories.api.client.IngameClient;
import xyz.chunkstories.api.plugin.ClientPluginManager;
import xyz.chunkstories.plugin.DefaultPluginManager;

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
