package io.xol.chunkstories.net;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import io.xol.chunkstories.api.net.PacketDefinition;

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
