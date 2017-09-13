package io.xol.chunkstories.server.commands;

import io.xol.chunkstories.api.plugin.commands.CommandHandler;
import io.xol.chunkstories.api.server.ServerInterface;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class ServerCommandBasic implements CommandHandler {
	protected final ServerInterface server;

	public ServerCommandBasic(ServerInterface server) {
		this.server = server;
	}
	
}
