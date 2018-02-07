package io.xol.chunkstories.net;

import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSendingContext;
import io.xol.chunkstories.api.net.packets.PacketDummy;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PacketOutgoingBuffered extends PacketDummy
{	
	int payload_size;
	
	public PacketOutgoingBuffered() {
		
	}
	
	public PacketOutgoingBuffered(byte[] data, int payload_size) {
		this.data = data;
		this.payload_size = payload_size;
	}

	@Override
	public void send(PacketDestinator destinator, DataOutputStream out, PacketSendingContext ctx) throws IOException
	{
		out.writeInt(payload_size);
		super.send(destinator, out, ctx);
	}
}
