package io.xol.chunkstories.net;

import java.io.DataInputStream;

public abstract class LogicalPacketDatagram {

	public final int packetTypeId, packetSize;
	
	public LogicalPacketDatagram(int packetTypeId, int packetSize) {
		this.packetTypeId = packetTypeId;
		this.packetSize = packetSize;
	}
	
	public abstract DataInputStream getData();

	public abstract void dispose();
}
