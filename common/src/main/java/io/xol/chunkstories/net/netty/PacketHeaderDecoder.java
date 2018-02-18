package io.xol.chunkstories.net.netty;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.xol.chunkstories.net.Connection;
import io.xol.chunkstories.net.LogicalPacketDatagram;

public class PacketHeaderDecoder extends ByteToMessageDecoder  {

	final Connection connection;
	
	private boolean streamingValve = false;
	
	public PacketHeaderDecoder(Connection connection) {
		this.connection = connection;
	}
	
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		if(streamingValve) {
			return;
		}
		
		if(in.readableBytes() < 1) return;
		int firstByte = in.readByte();
		int packetTypeId = 0;
		//If it is under 127 unsigned it's a 1-byte packet [0.firstByte(1.7)]
		if ((firstByte & 0x80) == 0)
			packetTypeId = firstByte;
		else
		{
			//It's a 2-byte packet [0.firstByte(1.7)][secondByte(0.8)]
			if(in.readableBytes() < 1) return;
			int secondByte = in.readByte();
			secondByte = secondByte & 0xFF;
			packetTypeId = secondByte | (firstByte & 0x7F) << 8;
		}

		if(in.readableBytes() < 4) return;
		int packetSize = in.readInt();
		
		if(in.readableBytes() >= packetSize) {
			ByteBuf buffer = ctx.alloc().buffer(packetSize);
			LogicalPacketDatagram datagram = new NettyPacketDatagram(packetTypeId, packetSize, buffer);
		}
		
		if(packetTypeId == 0x1) {
			//Special case for File streaming
			//ctx.pipeline().addBefore("headerDecoder", "fileReader", new FileStreamingHandler(connection));
			streamingValve = true;
			return;
		}
	}

}
