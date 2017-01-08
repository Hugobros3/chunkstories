package io.xol.chunkstories.net.packets;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.xol.chunkstories.api.exceptions.SyntaxErrorException;
import io.xol.chunkstories.api.mods.Asset;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.PacketSynch;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.net.ClientToServerConnection;
import io.xol.chunkstories.content.ModsManager;
import io.xol.chunkstories.content.ModsManager.AssetHierarchy;
import io.xol.chunkstories.server.Server;
import io.xol.chunkstories.server.net.ServerClient;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.engine.math.HexTools;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PacketsProcessor
{
	//There is 2^15 possible packets
	static PacketTypeLoaded[] packetTypes = new PacketTypeLoaded[32768];
	static Map<String, Short> packetIds = new HashMap<String, Short>();
	
	Queue<PendingSynchPacket> pendingSynchPackets = new ConcurrentLinkedQueue<PendingSynchPacket>();

	public static void loadPacketsTypes()
	{
		//Loads *all* possible packets types
		packetIds.clear();
		AssetHierarchy packetsFiles = ModsManager.getAssetInstances("./data/packetsTypes.txt");
		Iterator<Asset> i = packetsFiles.iterator();
		while(i.hasNext())
		{
			Asset a = i.next();
			loadPacketFile(a);
		}
	}

	private static void loadPacketFile(Asset f)
	{
		if (f == null)
			return;
		
		ChunkStoriesLogger.getInstance().log("Loading packets in "+f);
		try (BufferedReader reader = new BufferedReader(f.reader());)
		{
			String line = "";
			int ln = 0;
			while ((line = reader.readLine()) != null)
			{
				if (line.startsWith("#"))
				{
					// It's a comment, ignore.
				}
				else
				{
					if (line.startsWith("packet"))
					{
						String splitted[] = line.split(" ");
						if (!splitted[0].equals("packet"))
							throw new SyntaxErrorException(ln, f, "no packet line");
						short packetId = (short) HexTools.parseHexValue(splitted[1]);
						String packetName = splitted[2];
						try
						{
							Class<?> untypedClass = ModsManager.getClassByName(splitted[3]);
							if (!Packet.class.isAssignableFrom(untypedClass))
								throw new SyntaxErrorException(ln, f, splitted[3] + " is not a subclass of Packet");
							@SuppressWarnings("unchecked")
							Class<? extends Packet> packetClass = (Class<? extends Packet>) untypedClass;

							Class<?>[] types = { };
							Constructor<? extends Packet> constructor = packetClass.getConstructor(types);

							String allowed = splitted[4];

							PacketTypeLoaded packetType = new PacketTypeLoaded(packetId, packetName, packetClass, constructor, !allowed.equals("server"), !allowed.equals("client"));
							packetTypes[packetId] = packetType;
							packetIds.put(splitted[3], packetId);
						}
						catch (NoSuchMethodException | SecurityException | IllegalArgumentException e)
						{
							e.printStackTrace();
						}
					}
				}
				ln++;
			}
			//reader.close();
		}
		catch (IOException | SyntaxErrorException e)
		{
			ChunkStoriesLogger.getInstance().warning(e.getMessage());
		}
	}

	static class PacketTypeLoaded
	{
		short id;
		String packetName;
		Class<? extends Packet> packetClass;
		Constructor<? extends Packet> packetConstructor;
		boolean clientCanSendIt = true;
		boolean serverCanSendIt = true;

		public PacketTypeLoaded(short id, String packetName, Class<? extends Packet> packetClass, Constructor<? extends Packet> packetConstructor, boolean clientCanSendIt, boolean serverCanSendIt)
		{
			super();
			this.id = id;
			this.packetName = packetName;
			this.packetClass = packetClass;
			this.packetConstructor = packetConstructor;
			this.clientCanSendIt = clientCanSendIt;
			this.serverCanSendIt = serverCanSendIt;
		}

		public Packet createNew(boolean isClient, boolean sending) throws IllegalPacketException
		{
			Object[] parameters = {  };
			try
			{
				Packet packet = packetConstructor.newInstance(parameters);
				//Check legality
				if (sending)
				{
					if (isClient && !clientCanSendIt)
						throw new IllegalPacketException(packet);
					if (!isClient && !serverCanSendIt)
						throw new IllegalPacketException(packet);
				}
				//When receiving a packet
				else
				{
					if (isClient && !serverCanSendIt)
						throw new IllegalPacketException(packet);
					if (!isClient && !clientCanSendIt)
						throw new IllegalPacketException(packet);
				}
				return packet;
			}
			catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
			{
				ChunkStoriesLogger.getInstance().warning("Failed to instanciate " + packetName);
				e.printStackTrace();
			}
			return null;
		}
	}

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

	public PacketsProcessor(ClientToServerConnection serverConnection)
	{
		this.serverConnection = serverConnection;
		isClient = true;
	}

	public PacketsProcessor(ServerClient serverClient)
	{
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
			Packet packet = packetTypes[packetType].createNew(client, false);

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
		short id = getPacketId(packet);
		if (id < 127)
			out.writeByte((byte) id);
		else
		{
			out.writeByte((byte) (0x80 | id >> 8));
			out.writeByte((byte) (id % 256));
		}
	}

	public short getPacketId(Packet packet) throws UnknowPacketException
	{
		if (packet == null || !packetIds.containsKey(packet.getClass().getName()))
			throw new UnknowPacketException(-1);
		short id = packetIds.get(packet.getClass().getName());
		return id;
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
