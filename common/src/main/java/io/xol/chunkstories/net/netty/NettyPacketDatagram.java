package io.xol.chunkstories.net.netty;

import java.io.DataInputStream;

import io.netty.buffer.ByteBuf;
import io.xol.chunkstories.net.LogicalPacketDatagram;

public class NettyPacketDatagram extends LogicalPacketDatagram {

	public NettyPacketDatagram(int packetTypeId, int packetSize, ByteBuf buffer) {
		super(packetTypeId, packetSize);
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
