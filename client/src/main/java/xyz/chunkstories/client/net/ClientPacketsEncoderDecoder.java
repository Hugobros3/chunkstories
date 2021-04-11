//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.client.net;

import xyz.chunkstories.api.client.Client;
import xyz.chunkstories.api.client.IngameClient;
import xyz.chunkstories.api.client.LocalIngamePlayer;
import xyz.chunkstories.api.client.net.ClientPacketsProcessor;
import xyz.chunkstories.content.translator.InitialContentTranslator;
import xyz.chunkstories.net.PacketsEncoderDecoder;
import xyz.chunkstories.world.WorldClientRemote;

public class ClientPacketsEncoderDecoder extends PacketsEncoderDecoder implements ClientPacketsProcessor {

	private final Client client;
	private final ServerConnection serverConnection;

	public ClientPacketsEncoderDecoder(Client client, ServerConnection serverConnection) {
		super(client.getContent().getPackets(), serverConnection);

		this.client = client;
		this.serverConnection = serverConnection;

		// Very basic content translator used to translate the system packets (those with an assignated fixedId)
		// Gets replaced later during the connection process as we receive the actual mappings from the server !
		InitialContentTranslator translator = new InitialContentTranslator(client.getContent());
		this.contentTranslator = translator;
	}

	public ServerConnection getConnection() {
		return serverConnection;
	}

	@Override
	public WorldClientRemote getWorld() {
		if(getContext() == null)
			return null;
		return (WorldClientRemote) getContext().getWorld();
	}

	@Override
	public IngameClient getContext() {
		return client.getIngame();
	}

	public Client getClient() {
		return client;
	}

	@Override
	public Interlocutor getInterlocutor() {
		return serverConnection.getRemoteServer();
	}

	@Override
	public LocalIngamePlayer getPlayer() {
		return getContext().getPlayer();
	}

	@Override
	public boolean isServer() {
		return false;
	}
}
