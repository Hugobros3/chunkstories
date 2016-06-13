package io.xol.chunkstories.net.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.exceptions.PacketProcessingException;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSender;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public abstract class Packet
{
	//Who are we talking to ?
	private boolean isClient = false;
	private boolean isServer = false;
	
	public Packet(boolean client)
	{
		this.isClient = client;
		this.isServer = !client;
	}
	
	public abstract void send(PacketDestinator destinator, DataOutputStream out) throws IOException;
	
	public abstract void process(PacketSender sender, DataInputStream in, PacketsProcessor processor) throws IOException, PacketProcessingException;

	public boolean isSentFromClient()
	{
		return isClient;
	}

	public boolean isSentFromServer()
	{
		return isServer;
	}
}
