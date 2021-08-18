//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.net;

import java.io.DataInputStream;

public abstract class LogicalPacketDatagram {

	public final PacketDefinition packetDefinition;
	public final int packetSize;

	public LogicalPacketDatagram(PacketDefinition packetDefinition, int packetSize) {
		this.packetDefinition = packetDefinition;
		this.packetSize = packetSize;
	}

	public abstract DataInputStream getData();

	public abstract void dispose();
}
