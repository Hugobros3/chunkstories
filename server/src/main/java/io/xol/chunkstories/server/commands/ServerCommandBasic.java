//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.server.commands;

import io.xol.chunkstories.api.plugin.commands.CommandHandler;
import io.xol.chunkstories.api.server.ServerInterface;

public abstract class ServerCommandBasic implements CommandHandler {
	protected final ServerInterface server;

	public ServerCommandBasic(ServerInterface server) {
		this.server = server;
	}
	
}
