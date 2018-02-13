package io.xol.chunkstories.server.net;

import org.slf4j.Logger;

import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.server.ServerInterface;
import io.xol.chunkstories.api.server.ServerPacketsProcessor;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.net.PacketsProcessorCommon;
import io.xol.chunkstories.server.DedicatedServer;
import io.xol.chunkstories.server.player.ServerPlayer;

public class ServerPacketsProcessorImplementation implements ServerPacketsProcessor {

	final DedicatedServer server;
	
	public ServerPacketsProcessorImplementation(DedicatedServer server) {
		//super(server);
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
		return new UserPacketsProcessor(server, connection);
	}
	
	/*public PlayerPacketsProcessor forPlayer(ServerPlayer player)
	{
		return new PlayerPacketsProcessor(player);
	}*/

	public class UserPacketsProcessor extends PacketsProcessorCommon implements ServerPacketsProcessor {
		
		final UserConnection connection;
		//final Queue<PendingSynchPacket> pendingSynchPackets = new ConcurrentLinkedQueue<PendingSynchPacket>();
		
		public Logger logger() {
			return logger;
		}
		
		public UserPacketsProcessor(GameContext gameContext, UserConnection connection) {
			super(gameContext);
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

		/*public void sendPacketHeader(DataOutputStream out, Packet packet) throws UnknowPacketException, IOException {
			ServerPacketsProcessorImplementation.this.sendPacketHeader(out, packet);
		}*/

		/*public Packet getPacket_(DataInputStream in) throws IOException, UnknowPacketException, IllegalPacketException {
			//return ServerPacketsProcessorImplementation.this.getPacket(in);
			while (true)
			{
				
				
				Packet packet = ((PacketDefinitionImpl)store.getPacketTypeById(packetTypeId)).createNew(this instanceof ClientPacketsProcessor);

				//When we get a packetSynch
				if (packet instanceof PacketSynch)
				{
					//Read it's meta
					int packetSynchLength = in.readInt();

					//Read it entirely
					byte[] bufferedIncommingPacket = new byte[packetSynchLength];
					in.readFully(bufferedIncommingPacket);

					//Queue result
					pendingSynchPackets.add(new PendingSynchPacket(packet, bufferedIncommingPacket));
					
					//Skip this packet ( don't return it )
					continue;
				}

				if (packet == null)
					throw new UnknowPacketException(packetTypeId);
				else
					return packet;
			}
			//System.out.println("could not find packut");
			//throw new EOFException();
			
		}*/

		@Override
		public boolean isServer() {
			return true;
		}

		@Override
		public PacketSender getSender() {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException("TODO");
		}

		@Override
		public PacketDestinator getDestinator() {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException("TODO");
		}
	}
	
	public class PlayerPacketsProcessor extends UserPacketsProcessor implements ServerPlayerPacketsProcessor {
		final ServerPlayer player;
		
		public PlayerPacketsProcessor(ServerPlayer player) {
			super(player.getContext(), player.getPlayerConnection());
			this.player = player;
		}

		@Override
		public Player getPlayer() {
			return player;
		}
	}
}
