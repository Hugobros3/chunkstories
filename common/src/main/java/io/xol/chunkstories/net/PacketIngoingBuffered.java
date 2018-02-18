package io.xol.chunkstories.net;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

public class PacketIngoingBuffered extends LogicalPacketDatagram {

	DataInputStream dis;
	public PacketIngoingBuffered(int packetTypeId, int packetSize, byte[] data) {
		super(packetTypeId, packetSize);
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
