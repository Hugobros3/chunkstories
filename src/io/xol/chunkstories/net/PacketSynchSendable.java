package io.xol.chunkstories.net;

import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.net.packets.PacketDummy;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PacketSynchSendable extends PacketDummy
{
	public short packetType;
	public int packetLength;
	
	@Override
	public void send(PacketDestinator destinator, DataOutputStream out) throws IOException
	{
		//Send packet header here
		//See PacketProcessor.sendPacketHeader()
		
		if(packetType < 127)
			out.writeByte((byte)packetType);
		else
		{
			out.writeByte((byte)(0x80 | packetType >> 8));
			out.writeByte((byte)(packetType % 256));
		}
		
		out.writeInt(packetLength);
		super.send(destinator, out);
	}
}
