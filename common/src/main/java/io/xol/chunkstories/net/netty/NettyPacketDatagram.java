package io.xol.chunkstories.net.netty;

import java.io.DataInputStream;

import io.netty.buffer.ByteBuf;
import io.xol.chunkstories.api.net.PacketDefinition;
import io.xol.chunkstories.net.LogicalPacketDatagram;

public class NettyPacketDatagram extends LogicalPacketDatagram {

	

	public NettyPacketDatagram(PacketDefinition packetDefinition, int packetSize) {
		super(packetDefinition, packetSize);
		// TODO Auto-generated constructor stub
	}

	@Override
	public DataInputStream getData() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

}
