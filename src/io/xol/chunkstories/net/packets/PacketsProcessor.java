package io.xol.chunkstories.net.packets;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.xol.chunkstories.api.Content;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.PacketSynch;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.net.ClientToServerConnection;
import io.xol.chunkstories.server.Server;
import io.xol.chunkstories.server.net.ServerClient;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PacketsProcessor
{
	private final Content.PacketTypes store;
	
	Queue<PendingSynchPacket> pendingSynchPackets = new ConcurrentLinkedQueue<PendingSynchPacket>();

	//Both clients and server use this class
	ClientToServerConnection serverConnection;
	ServerClient serverClient;

	public ClientToServerConnection getClientToServerConnection()
	{
		return serverConnection;
	}

	public ServerClient getServerClient()
	{
		return serverClient;
	}

	boolean isClient = false;

	public PacketsProcessor(ClientToServerConnection serverConnection, Content.PacketTypes store)
	{
		this.store = store;
		this.serverConnection = serverConnection;
		isClient = true;
	}

	public PacketsProcessor(ServerClient serverClient, Content.PacketTypes store)
	{
		this.store = store;
		this.serverClient = serverClient;
		isClient = false;
	}

	/**
	 * Read 1 or 2 bytes to get the next packet ID and returns a packet of this type if it exists
	 * 
	 * @param in
	 *            The InputStream of the connection
	 * @param client
	 *            Wether a client or a server has to process this packet
	 * @param sending
	 *            Wether we are receiving or sending this packet
	 * @return A valid Packet
	 * @throws IOException
	 *             If the stream dies when we process it
	 * @throws UnknowPacketException
	 *             If the packet id we obtain is invalid
	 * @throws IllegalPacketException
	 *             If the packet we obtain is illegal ( if we're not supposed to receive or send it )
	 */
	public Packet getPacket(DataInputStream in, boolean client) throws IOException, UnknowPacketException, IllegalPacketException
	{
		while (true)
		{
			int firstByte = in.readByte();
			int packetType = 0;
			//If it is under 127 unsigned it's a 1-byte packet [0.firstByte(1.7)]
			if ((firstByte & 0x80) == 0)
				packetType = firstByte;
			else
			{
				//It's a 2-byte packet [0.firstByte(1.7)][secondByte(0.8)]
				int secondByte = in.readByte();
				secondByte = secondByte & 0xFF;
				packetType = secondByte | (firstByte & 0x7F) << 8;
			}
			Packet packet = store.createPacketById(packetType);

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
				throw new UnknowPacketException(packetType);
			else
				return packet;
		}
		//System.out.println("could not find packut");
		//throw new EOFException();
	}

	/**
	 * Sends the packets header ( ID )
	 * 
	 * @param out
	 * @param packet
	 * @throws UnknowPacketException
	 * @throws IOException
	 */
	public void sendPacketHeader(DataOutputStream out, Packet packet) throws UnknowPacketException, IOException
	{
		short id = store.getPacketId(packet);
		if (id < 127)
			out.writeByte((byte) id);
		else
		{
			out.writeByte((byte) (0x80 | id >> 8));
			out.writeByte((byte) (id % 256));
		}
	}

	public World getWorld()
	{
		if (this.isClient)
			return Client.world;
		else
			return Server.getInstance().getWorld();
	}
	
	public PendingSynchPacket getPendingSynchPacket()
	{
		return pendingSynchPackets.poll();
	}

	public class PendingSynchPacket
	{
		Packet packet;

		ByteArrayInputStream bais;
		DataInputStream dis;

		public PendingSynchPacket(Packet packet, byte[] buffer)
		{
			this.packet = packet;

			this.bais = new ByteArrayInputStream(buffer);
			this.dis = new DataInputStream(bais);
		}

		public void process(PacketSender sender, PacketsProcessor processor)
		{
			try
			{
				//System.out.println(packet.getClass().getSimpleName());
				packet.process(sender, dis, processor);
			}
			catch (Exception e)
			{
				/*if (!die) // If the thread was killed then there is no point
							// handling the error.
				{
					// close();
					failed = true;
					latestErrorMessage = "Fatal error while handling connection to " + ip + ":" + port + ". (" + e.getClass().getName() + ")";
					System.out.println(latestErrorMessage);
					close();
					e.printStackTrace();
				}*/
				e.printStackTrace();
			}
		}
	}
}
