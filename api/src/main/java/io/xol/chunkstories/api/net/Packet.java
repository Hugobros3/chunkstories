package io.xol.chunkstories.api.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.exceptions.PacketProcessingException;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

/** Packets are atomic data bits used to communicate over the network */
public abstract class Packet
{
	public Packet()
	{
		
	}
	
	/** Calld at send time, be consistent with the data you send, and give yourself a way to know how many bytes to expect if it's a variable length */
	public abstract void send(PacketDestinator destinator, DataOutputStream out) throws IOException;
	
	/** Called at reception, has to read the exact number of bytes sent or bad stuff happens ! */
	public abstract void process(PacketSender sender, DataInputStream in, PacketsProcessor processor) throws IOException, PacketProcessingException;
}
