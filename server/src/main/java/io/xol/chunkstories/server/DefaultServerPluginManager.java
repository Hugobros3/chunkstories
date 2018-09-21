//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.server;

import io.xol.chunkstories.api.plugin.ServerPluginManager;
import io.xol.chunkstories.api.server.Server;
import io.xol.chunkstories.plugin.DefaultPluginManager;

public class DefaultServerPluginManager extends DefaultPluginManager implements ServerPluginManager {
	Server server;

	public DefaultServerPluginManager(Server server) {
		super(server);
		this.server = server;
	}

	@Override
	public Server getServer() {
		return server;
	}
}
