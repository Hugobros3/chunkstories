package io.xol.chunkstories.api.plugin.commands;

import io.xol.chunkstories.api.server.ServerInterface;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface ServerConsole extends CommandEmitter {
	public ServerInterface getServer();
}
