//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.content.Asset;
import io.xol.chunkstories.api.content.Content;
import io.xol.chunkstories.api.exceptions.content.IllegalPacketDeclarationException;
import io.xol.chunkstories.api.exceptions.net.UnknowPacketException;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.net.PacketDefinition;
import io.xol.chunkstories.content.GameContentStore;

public class PacketsStore implements Content.PacketDefinitions {

	private final GameContentStore store;
	private final Map<String, PacketDefinitionImplementation> byNames = new HashMap<String, PacketDefinitionImplementation>();
	private final Map<Class<? extends Packet>, PacketDefinitionImplementation> byClasses = new HashMap<Class<? extends Packet>, PacketDefinitionImplementation>();

	// private PacketDefinition textPacket, filePacket;

	private static final Logger logger = LoggerFactory.getLogger("content.packets");

	public Logger logger() {
		return logger;
	}

	public PacketsStore(GameContentStore store) {
		this.store = store;

		// reload();
	}

	public void reload() {

		byNames.clear();
		byClasses.clear();

		// Load system.packets
		InputStream is = getClass().getResourceAsStream("/system.packets");
		readPacketsDefinitions(new BufferedReader(new InputStreamReader(is)), new Object() {
			@Override
			public String toString() {
				return "system.packets";
			}
		});
		//TODO review this system
	}

	private void readPacketsDefinitions(Asset f) {
		if (f == null)
			return;
		BufferedReader reader = new BufferedReader(f.reader());
		readPacketsDefinitions(reader, f);
	}

	private void readPacketsDefinitions(BufferedReader reader, Object source) {
		logger().debug("Reading packets definitions in : " + source);

		//TODO
		//throw new UnsupportedOperationException("TODO");
		/*try {
			String line = "";
			PacketDefinitionImplementation packetType = null;
			while ((line = reader.readLine()) != null) {
				line = line.replace("\t", "");
				if (line.startsWith("#")) {
					// It's a comment, ignore.
				}
				// We shouldn't come accross end tags by ourselves, this is
				// dealt with in the constructors
				else if (line.startsWith("end")) {
					logger().warn("Syntax error in file : " + source + " : Unexpected 'end' tag.");
					continue;
				} else if (line.startsWith("packet")) {
					if (line.contains(" ")) {
						String[] split = line.split(" ");
						String materialName = split[1];

						try {
							packetType = new PacketDefinitionImplementation(store, materialName, reader);
						} catch (IllegalPacketDeclarationException e) {
							store.logger().error(e.getMessage());
							continue;
						}

						// Eventually add the packet type
						byNames.put(packetType.getName(), packetType);

						// Add quick-resolve hashmap entries
						if (packetType.clientClass != null)
							this.byClasses.put(packetType.clientClass, packetType);
						if (packetType.serverClass != null)
							this.byClasses.put(packetType.serverClass, packetType);
						if (packetType.commonClass != null)
							this.byClasses.put(packetType.commonClass, packetType);
					}
				}
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}*/
	}

	@Override
	public PacketDefinition getPacketByName(String name) {
		return byNames.get(name);
	}

	@Override
	public PacketDefinition getPacketFromInstance(Packet packet) throws UnknowPacketException {
		Class<? extends Packet> pclass = packet.getClass();

		PacketDefinitionImplementation ptd = this.byClasses.get(pclass);
		if (ptd != null)
			return ptd;

		throw new UnknowPacketException(packet);
	}

	@Override
	public Content parent() {
		return this.store;
	}

	@Override
	public Iterator<PacketDefinition> all() {
		Iterator<PacketDefinitionImplementation> i = byNames.values().iterator();
		return new Iterator<PacketDefinition>() {

			@Override
			public boolean hasNext() {
				return i.hasNext();
			}

			@Override
			public PacketDefinition next() {
				return i.next();
			}

		};
	}

	/*
	 * @Override public PacketDefinition getTextPacket() { return textPacket; }
	 * 
	 * @Override public PacketDefinition getFilePacket() { return filePacket; }
	 */
}
