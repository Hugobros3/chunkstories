//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.client.net;

import xyz.chunkstories.api.client.Client;
import xyz.chunkstories.client.ClientImplementation;
import xyz.chunkstories.client.ingame.IngameClientImplementation;
import xyz.chunkstories.content.GameContentStore;
import xyz.chunkstories.content.translator.InitialContentTranslator;
import xyz.chunkstories.net.PacketsEncoderDecoder;
import xyz.chunkstories.world.WorldImplementation;

public class ClientPacketsEncoderDecoder extends PacketsEncoderDecoder {

	private final ClientImplementation client;
	private final ServerConnection serverConnection;

	public ClientPacketsEncoderDecoder(ClientImplementation client, ServerConnection serverConnection) {
		super(((GameContentStore)client.getContent()).getPackets(), serverConnection);

		this.client = client;
		this.serverConnection = serverConnection;

		// Very basic content translator used to translate the system packets (those with an assignated fixedId)
		// Gets replaced later during the connection process as we receive the actual mappings from the server !
		InitialContentTranslator translator = new InitialContentTranslator((GameContentStore) client.getContent());
		this.contentTranslator = translator;
	}

	@Override
	public WorldImplementation getWorld() {
		if(getContext() == null)
			return null;
		return getContext().getWorld();
	}

	//@Override
	public IngameClientImplementation getContext() {
		return client.getIngame();
	}

	public Client getClient() {
		return client;
	}
}
