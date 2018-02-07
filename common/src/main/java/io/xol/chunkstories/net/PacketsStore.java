package io.xol.chunkstories.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.content.Content;
import io.xol.chunkstories.api.exceptions.content.IllegalPacketDeclarationException;
import io.xol.chunkstories.api.exceptions.net.UnknowPacketException;
import io.xol.chunkstories.api.content.Asset;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.net.PacketDefinition;
import io.xol.chunkstories.content.GameContentStore;
public class PacketsStore implements Content.PacketTypes {

	private final GameContentStore store;

	private final Map<String, PacketDefinitionImpl> byNames = new HashMap<String, PacketDefinitionImpl>();
	//private final PacketDefinitionImpl[] byIDs = new PacketDefinitionImpl[32768];

	private final Map<Class<? extends Packet>, PacketDefinitionImpl> byClasses = new HashMap<Class<? extends Packet>, PacketDefinitionImpl>();
	
	private static final Logger logger = LoggerFactory.getLogger("content.packets");
	public Logger logger() {
		return logger;
	}
	
	public PacketsStore(GameContentStore store) {
		this.store = store;

		//reload();
	}

	public void reload() {

		byNames.clear();
		byClasses.clear();
		//for (int i = 0; i < byIDs.length; i++)
		//	byIDs[i] = null;

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

			PacketDefinitionImpl packetType = null;
			while ((line = reader.readLine()) != null) {
				line = line.replace("\t", "");
				if (line.startsWith("#")) {
					// It's a comment, ignore.
				}
				// We shouldn't come accross end tags by ourselves, this is
				// dealt with in the constructors
				else if (line.startsWith("end")) {
					logger()
							.warn("Syntax error in file : " + f + " : Unexpected 'end' tag.");
					continue;
				} else if (line.startsWith("packet")) {
					if (line.contains(" ")) {
						String[] split = line.split(" ");
						String materialName = split[1];
						int id = Integer.parseInt(split[2]);

						try {
							packetType = new PacketDefinitionImpl(store, materialName, id, reader);
						} catch (IllegalPacketDeclarationException e) {
							store.logger().error(e.getMessage());
							continue;
						}

						// Eventually add the packet type
						byNames.put(packetType.getName(), packetType);
						//byIDs[packetType.getID()] = packetType;
						
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

	/*@Override
	public PacketType getPacketTypeById(int packetID) {
		return byIDs[packetID];
	}*/

	@Override
	public PacketDefinition getPacketTypeByName(String name) {
		return byNames.get(name);
	}

	@Override
	public PacketDefinition getPacketType(Packet packet) throws UnknowPacketException {
		Class<? extends Packet> pclass = packet.getClass();
		
		PacketDefinitionImpl ptd = this.byClasses.get(pclass);
		if(ptd != null)
			return ptd;
		
		throw new UnknowPacketException(packet);
	}

	@Override
	public Content parent() {
		return this.store;
	}

}
