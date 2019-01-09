//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.net;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import xyz.chunkstories.api.net.PacketDefinition;

public class PacketIngoingBuffered extends LogicalPacketDatagram {

	DataInputStream dis;

	public PacketIngoingBuffered(PacketDefinition packetDefinition, int packetSize, byte[] data) {
		super(packetDefinition, packetSize);
		this.dis = new DataInputStream(new ByteArrayInputStream(data));
	}

	@Override
	public DataInputStream getData() {
		return dis;
	}

	@Override
	public void dispose() {

	}
}
