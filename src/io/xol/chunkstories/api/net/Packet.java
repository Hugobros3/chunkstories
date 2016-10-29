package io.xol.chunkstories.api.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.exceptions.PacketProcessingException;
import io.xol.chunkstories.net.packets.PacketsProcessor;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public abstract class Packet
{
	public Packet()
	{
		
	}
	
	public abstract void send(PacketDestinator destinator, DataOutputStream out) throws IOException;
	
	//TODO Make that an alternative, mandatory constructor
	public abstract void process(PacketSender sender, DataInputStream in, PacketsProcessor processor) throws IOException, PacketProcessingException;
}
