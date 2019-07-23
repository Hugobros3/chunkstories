//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.net;

import xyz.chunkstories.api.content.Content;
import org.slf4j.Logger;

import xyz.chunkstories.api.content.OnlineContentTranslator;
import xyz.chunkstories.api.net.Interlocutor;
import xyz.chunkstories.api.player.Player;
import xyz.chunkstories.api.server.Server;
import xyz.chunkstories.api.server.ServerPacketsProcessor;
import xyz.chunkstories.net.PacketsEncoderDecoder;
import xyz.chunkstories.server.DedicatedServer;
import xyz.chunkstories.server.player.ServerPlayer;
import xyz.chunkstories.world.WorldServer;

public class ServerPacketsProcessorImplementation implements ServerPacketsProcessor {

	final DedicatedServer server;

	public ServerPacketsProcessorImplementation(DedicatedServer server) {
		this.server = server;
	}

	@Override
	public Server getContext() {
		return server;
	}

	@Override
	public WorldServer getWorld() {
		return server.getWorld();
	}

	public ClientPacketsContext forConnection(ClientConnection connection) {
		return new ClientPacketsContext(server.getContent().getPackets(), connection);
	}

	/** Processes the packets for a certain user connection */
	public class ClientPacketsContext extends PacketsEncoderDecoder implements ServerPacketsProcessor {

		final ClientConnection connection;

		public Logger logger() {
			return logger;
		}

		public ClientPacketsContext(Content.PacketDefinitions packetDefinitions, ClientConnection connection) {
			super(packetDefinitions, connection);
			this.connection = connection;
		}

		public OnlineContentTranslator getContentTranslator() {
			return getWorld().getContentTranslator();
		}

		public ClientConnection getConnection() {
			return connection;
		}

		public PlayerPacketsProcessor toPlayer(ServerPlayer player) {
			return new PlayerPacketsProcessor(player);
		}

		@Override
		public WorldServer getWorld() {
			return ServerPacketsProcessorImplementation.this.getWorld();
		}

		@Override
		public Server getContext() {
			return ServerPacketsProcessorImplementation.this.getContext();
		}

		@Override
		public boolean isServer() {
			return true;
		}

		@Override
		public Interlocutor getInterlocutor() {
			return connection;
		}
	}

	/** Processes the packets sent/received by an authenticated *player* */
	public class PlayerPacketsProcessor extends ClientPacketsContext implements ServerPlayerPacketsProcessor {
		final ServerPlayer player;

		public PlayerPacketsProcessor(ServerPlayer player) {
			super(server.getContent().getPackets(), player.getPlayerConnection());
			this.player = player;
		}

		@Override
		public Player getPlayer() {
			return player;
		}

		@Override
		public Interlocutor getInterlocutor() {
			return player;
		}
	}
}
