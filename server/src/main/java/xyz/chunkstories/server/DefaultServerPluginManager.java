//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server;

import xyz.chunkstories.api.plugin.ServerPluginManager;
import xyz.chunkstories.api.server.Server;
import xyz.chunkstories.plugin.DefaultPluginManager;

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
