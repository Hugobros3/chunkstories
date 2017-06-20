package io.xol.chunkstories.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.xol.chunkstories.api.Content;
import io.xol.chunkstories.api.exceptions.content.IllegalPacketDeclarationException;
import io.xol.chunkstories.api.exceptions.net.UnknowPacketException;
import io.xol.chunkstories.api.mods.Asset;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.content.GameContentStore;
import io.xol.chunkstories.tools.ChunkStoriesLoggerImplementation;

public class PacketsStore implements Content.PacketTypes {

	private final GameContentStore store;

	private final Map<String, PacketTypeDeclared> byNames = new HashMap<String, PacketTypeDeclared>();
	private final PacketTypeDeclared[] byIDs = new PacketTypeDeclared[32768];

	private final Map<Class<? extends Packet>, PacketTypeDeclared> byClasses = new HashMap<Class<? extends Packet>, PacketTypeDeclared>();

	public PacketsStore(GameContentStore store) {
		this.store = store;

		//reload();
	}

	public void reload() {

		byNames.clear();
		byClasses.clear();
		for (int i = 0; i < byIDs.length; i++)
			byIDs[i] = null;

		Iterator<Asset> i = store.modsManager().getAllAssetsByExtension("packets");
		while (i.hasNext()) {
			Asset f = i.next();
			readPacketsDefinitions(f);
		}
	}

	private void readPacketsDefinitions(Asset f) {
		if (f == null)
			return;
		try {
			BufferedReader reader = new BufferedReader(f.reader());
			String line = "";

			PacketTypeDeclared packetType = null;
			while ((line = reader.readLine()) != null) {
				line = line.replace("\t", "");
				if (line.startsWith("#")) {
					// It's a comment, ignore.
				}
				// We shouldn't come accross end tags by ourselves, this is
				// dealt with in the constructors
				else if (line.startsWith("end")) {
					ChunkStoriesLoggerImplementation.getInstance()
							.warning("Syntax error in file : " + f + " : Unexpected 'end' tag.");
					continue;
				} else if (line.startsWith("packet")) {
					if (line.contains(" ")) {
						String[] split = line.split(" ");
						String materialName = split[1];
						int id = Integer.parseInt(split[2]);

						try {
							packetType = new PacketTypeDeclared(store, materialName, id, reader);
						} catch (IllegalPacketDeclarationException e) {
							store.logger().error(e.getMessage());
							continue;
						}

						// Eventually add the packet type
						byNames.put(packetType.getName(), packetType);
						byIDs[packetType.getID()] = packetType;
						
						//Add quick-resolve hashmap entries
						if(packetType.clientClass != null)
							this.byClasses.put(packetType.clientClass, packetType);
						if(packetType.serverClass != null)
							this.byClasses.put(packetType.serverClass, packetType);
						if(packetType.commonClass != null)
							this.byClasses.put(packetType.commonClass, packetType);
					}
				}
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public PacketType getPacketTypeById(int packetID) {
		return byIDs[packetID];
	}

	@Override
	public PacketType getPacketTypeByName(String name) {
		return byNames.get(name);
	}

	@Override
	public PacketType getPacketType(Packet packet) throws UnknowPacketException {
		Class<? extends Packet> pclass = packet.getClass();
		
		PacketTypeDeclared ptd = this.byClasses.get(pclass);
		if(ptd != null)
			return ptd;
		
		throw new UnknowPacketException(packet);
	}

	@Override
	public Content parent() {
		return this.store;
	}

}
