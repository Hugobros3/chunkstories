package io.xol.chunkstories.client.net;

import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.client.net.ClientPacketsProcessor;
import io.xol.chunkstories.api.net.Interlocutor;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.net.PacketsContextCommon;
import io.xol.chunkstories.world.WorldClientRemote;

public class ClientPacketsContext extends PacketsContextCommon implements ClientPacketsProcessor {

	final ClientInterface client;
	final ServerConnection clientConnection;
	
	public ClientPacketsContext(ClientInterface gameContext, ServerConnection clientConnection) {
		super(gameContext, clientConnection);
		
		this.client = gameContext;
		this.clientConnection = clientConnection;
	}
	
	public ServerConnection getConnection() {
		return clientConnection;
	}

	@Override
	public WorldClientRemote getWorld() {
		return (WorldClientRemote) client.getWorld();
	}

	@Override
	public ClientInterface getContext() {
		return client;
	}

	@Override
	public Interlocutor getInterlocutor() {
		return clientConnection.getRemoteServer();
	}

	@Override
	public Player getPlayer() {
		return client.getPlayer();
	}

	@Override
	public boolean isServer() {
		return false;
	}
}
