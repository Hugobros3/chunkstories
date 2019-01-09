//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.client.net;

import java.io.File;

import xyz.chunkstories.api.GameContext;
import xyz.chunkstories.api.net.RemoteServer;
import xyz.chunkstories.net.Connection;

public abstract class ServerConnection extends Connection {
	public ServerConnection(String remoteAddress, int port) {
		super(remoteAddress, port);
	}

	public abstract RemoteServer getRemoteServer();

	public abstract ConnectionStep obtainModFile(String modMd5Hash, File cached);
}