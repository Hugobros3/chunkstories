package io.xol.chunkstories.server.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.exceptions.net.IllegalPacketException;
import io.xol.chunkstories.api.exceptions.net.UnknowPacketException;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.net.PacketsProcessor;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.server.ServerInterface;
import io.xol.chunkstories.api.server.ServerPacketsProcessor;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.net.PacketsProcessorCommon;
import io.xol.chunkstories.server.Server;
import io.xol.chunkstories.server.ServerPlayer;

public class ServerPacketsProcessorImplementation extends PacketsProcessorCommon implements ServerPacketsProcessor {

	final Server server;
	
	public ServerPacketsProcessorImplementation(Server server) {
		super(server);
		this.server = server;
	}

	@Override
	public ServerInterface getContext() {
		return server;
	}

	@Override
	public WorldMaster getWorld() {
		return server.getWorld();
	}
	
	public UserPacketsProcessor forConnection(UserConnection connection)
	{
		return new UserPacketsProcessor(connection);
	}
	
	/*public PlayerPacketsProcessor forPlayer(ServerPlayer player)
	{
		return new PlayerPacketsProcessor(player);
	}*/

	public class UserPacketsProcessor implements PacketsProcessor, ServerPacketsProcessor {
		
		final UserConnection connection;
		
		public UserPacketsProcessor(UserConnection connection) {
			this.connection = connection;
		}
		
		public UserConnection getConnection()
		{
			return connection;
		}
		
		public PlayerPacketsProcessor toPlayer(ServerPlayer player) {
			return new PlayerPacketsProcessor(player);
		}
		
		@Override
		public WorldMaster getWorld() {
			return ServerPacketsProcessorImplementation.this.getWorld();
		}

		@Override
		public ServerInterface getContext() {
			return ServerPacketsProcessorImplementation.this.getContext();
		}

		public void sendPacketHeader(DataOutputStream out, Packet packet) throws UnknowPacketException, IOException {
			ServerPacketsProcessorImplementation.this.sendPacketHeader(out, packet);
		}

		public Packet getPacket(DataInputStream in) throws IOException, UnknowPacketException, IllegalPacketException {
			return ServerPacketsProcessorImplementation.this.getPacket(in);
		}
	}
	
	public class PlayerPacketsProcessor extends UserPacketsProcessor implements ServerPlayerPacketsProcessor {
		final ServerPlayer player;
		
		public PlayerPacketsProcessor(ServerPlayer player) {
			super(player.getPlayerConnection());
			this.player = player;
		}

		@Override
		public Player getPlayer() {
			return player;
		}
	}
}
