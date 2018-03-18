//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.client.net;

import java.io.File;

import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.net.RemoteServer;
import io.xol.chunkstories.net.Connection;

public abstract class ServerConnection extends Connection {
	
	public ServerConnection(GameContext gameContext, String remoteAddress, int port) {
		super(gameContext, remoteAddress, port);
	}
	
	public abstract RemoteServer getRemoteServer();

	public abstract ConnectionStep obtainModFile(String modMd5Hash, File cached);
}