//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.client.net;

import io.xol.chunkstories.api.client.IngameClient;
import io.xol.chunkstories.api.client.LocalPlayer;
import io.xol.chunkstories.api.client.net.ClientPacketsProcessor;
import io.xol.chunkstories.api.net.Interlocutor;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.content.translator.InitialContentTranslator;
import io.xol.chunkstories.net.PacketsContextCommon;
import io.xol.chunkstories.world.WorldClientRemote;

public class ClientPacketsContext extends PacketsContextCommon implements ClientPacketsProcessor {

	final IngameClient client;
	final ServerConnection clientConnection;

	public ClientPacketsContext(IngameClient gameContext, ServerConnection clientConnection) {
		super(gameContext, clientConnection);

		this.client = gameContext;
		this.clientConnection = clientConnection;

		InitialContentTranslator translator = new InitialContentTranslator(gameContext.getContent());
		// translator.assignPacketIds();
		// translator.buildArrays();
		this.contentTranslator = translator;
	}

	public ServerConnection getConnection() {
		return clientConnection;
	}

	@Override
	public WorldClientRemote getWorld() {
		return (WorldClientRemote) client.getWorld();
	}

	@Override
	public IngameClient getContext() {
		return client;
	}

	@Override
	public Interlocutor getInterlocutor() {
		return clientConnection.getRemoteServer();
	}

	@Override
	public LocalPlayer getPlayer() {
		return client.getPlayer();
	}

	@Override
	public boolean isServer() {
		return false;
	}
}
