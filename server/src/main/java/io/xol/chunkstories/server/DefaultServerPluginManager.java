//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.server;

import io.xol.chunkstories.api.plugin.ServerPluginManager;
import io.xol.chunkstories.api.server.ServerInterface;
import io.xol.chunkstories.plugin.DefaultPluginManager;

public class DefaultServerPluginManager extends DefaultPluginManager implements ServerPluginManager {
	ServerInterface server;

	public DefaultServerPluginManager(ServerInterface server) {
		super(server);
		this.server = server;
	}

	@Override
	public ServerInterface getServerInterface() {
		return server;
	}
}
