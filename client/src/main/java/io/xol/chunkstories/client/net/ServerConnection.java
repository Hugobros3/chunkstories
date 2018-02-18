package io.xol.chunkstories.client.net;

import java.io.File;

import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.net.RemoteServer;
import io.xol.chunkstories.net.Connection;
import io.xol.engine.misc.ConnectionStep;

public abstract class ServerConnection extends Connection {
	
	public ServerConnection(GameContext gameContext, String remoteAddress, int port) {
		super(gameContext, remoteAddress, port);
	}
	
	public abstract RemoteServer getRemoteServer();

	public abstract ConnectionStep obtainModFile(String modMd5Hash, File cached);
}