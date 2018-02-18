package io.xol.chunkstories.server.net;

import org.slf4j.Logger;

import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.content.OnlineContentTranslator;
import io.xol.chunkstories.api.net.Interlocutor;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.server.ServerInterface;
import io.xol.chunkstories.api.server.ServerPacketsProcessor;
import io.xol.chunkstories.net.PacketsContextCommon;
import io.xol.chunkstories.server.DedicatedServer;
import io.xol.chunkstories.server.player.ServerPlayer;
import io.xol.chunkstories.world.WorldServer;

public class ServerPacketsProcessorImplementation implements ServerPacketsProcessor {

	final DedicatedServer server;
	
	public ServerPacketsProcessorImplementation(DedicatedServer server) {
		this.server = server;
	}

	@Override
	public ServerInterface getContext() {
		return server;
	}

	@Override
	public WorldServer getWorld() {
		return server.getWorld();
	}
	
	public ClientPacketsContext forConnection(ClientConnection connection)
	{
		return new ClientPacketsContext(server, connection);
	}
	
	public class ClientPacketsContext extends PacketsContextCommon implements ServerPacketsProcessor {
		
		final ClientConnection connection;
		
		public Logger logger() {
			return logger;
		}
		
		public ClientPacketsContext(GameContext gameContext, ClientConnection connection) {
			super(gameContext, connection);
			this.connection = connection;
		}
		
		public OnlineContentTranslator getContentTranslator() {
			return getWorld().getContentTranslator();
		}
		
		public ClientConnection getConnection()
		{
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
		public ServerInterface getContext() {
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
	
	public class PlayerPacketsProcessor extends ClientPacketsContext implements ServerPlayerPacketsProcessor {
		final ServerPlayer player;
		
		public PlayerPacketsProcessor(ServerPlayer player) {
			super(player.getContext(), player.getPlayerConnection());
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
