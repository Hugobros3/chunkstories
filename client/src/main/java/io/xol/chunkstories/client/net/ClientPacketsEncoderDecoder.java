//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.client.net;

import io.xol.chunkstories.api.client.Client;
import io.xol.chunkstories.api.client.IngameClient;
import io.xol.chunkstories.api.client.LocalPlayer;
import io.xol.chunkstories.api.client.net.ClientPacketsProcessor;
import io.xol.chunkstories.api.net.Interlocutor;
import io.xol.chunkstories.content.translator.InitialContentTranslator;
import io.xol.chunkstories.net.PacketsEncoderDecoder;
import io.xol.chunkstories.world.WorldClientRemote;

public class ClientPacketsEncoderDecoder extends PacketsEncoderDecoder implements ClientPacketsProcessor {

	private IngameClient client = null;
	final ServerConnection clientConnection;

	public ClientPacketsEncoderDecoder(Client client, ServerConnection clientConnection) {
		super(client.getContent().packets(), clientConnection);

		this.clientConnection = clientConnection;

		// Very basic content translator used to translate the system packets (those with an assignated fixedId)
		// Gets replaced later during the connection process as we receive the actual mappings from the server !
		InitialContentTranslator translator = new InitialContentTranslator(client.getContent());
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
