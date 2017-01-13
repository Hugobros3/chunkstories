package io.xol.chunkstories.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.xol.chunkstories.api.Content;
import io.xol.chunkstories.api.Content.PacketTypes;
import io.xol.chunkstories.api.exceptions.SyntaxErrorException;
import io.xol.chunkstories.api.mods.Asset;
import io.xol.chunkstories.api.mods.AssetHierarchy;
import io.xol.chunkstories.api.mods.ModsManager;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.content.GameContentStore;
import io.xol.chunkstories.net.packets.IllegalPacketException;
import io.xol.chunkstories.net.packets.UnknowPacketException;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.engine.math.HexTools;

public class PacketsStore implements Content.PacketTypes
{
	private final GameContentStore store;
	private final ModsManager modsManager;
	
	public PacketsStore(GameContentStore store)
	{
		this.store = store;
		this.modsManager = store.modsManager();
		
		reload();
	}
	
	//There is 2^15 possible packets
	PacketTypeLoaded[] packetTypes = new PacketTypeLoaded[32768];
	Map<String, Short> packetIds = new HashMap<String, Short>();

	public void reload()
	{
		//Loads *all* possible packets types
		packetIds.clear();
		AssetHierarchy packetsFiles = modsManager.getAssetInstances("./data/packetsTypes.txt");
		Iterator<Asset> i = packetsFiles.iterator();
		while (i.hasNext())
		{
			Asset a = i.next();
			loadPacketFile(a);
		}
	}

	private void loadPacketFile(Asset f)
	{
		if (f == null)
			return;

		ChunkStoriesLogger.getInstance().log("Loading packets in " + f);
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
							Class<?> untypedClass = modsManager.getClassByName(splitted[3]);
							if (!Packet.class.isAssignableFrom(untypedClass))
								throw new SyntaxErrorException(ln, f, splitted[3] + " is not a subclass of Packet");
							@SuppressWarnings("unchecked")
							Class<? extends Packet> packetClass = (Class<? extends Packet>) untypedClass;

							Class<?>[] types = {};
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

	public short getPacketId(Packet packet) throws UnknowPacketException
	{
		if (packet == null || !packetIds.containsKey(packet.getClass().getName()))
			throw new UnknowPacketException(-1);
		short id = packetIds.get(packet.getClass().getName());
		return id;
	}

	class PacketTypeLoaded
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

		public Packet createNew() throws IllegalPacketException
		{
			Object[] parameters = {};
			try
			{
				Packet packet = packetConstructor.newInstance(parameters);
				//Check legality
				/*if (sending)
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
				}*/
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

	@Override
	public Packet createPacketById(int packetID) throws IllegalPacketException
	{
		return packetTypes[packetID].createNew();
	}

	@Override
	public Content parent()
	{
		return store;
	}
}
