package io.xol.chunkstories.net.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public abstract class Packet
{
	public boolean client = false;
	
	public Packet(boolean client)
	{
		this.client = client;
	}
	
	public abstract void send(DataOutputStream out) throws IOException;
	
	public abstract void read(DataInputStream in) throws IOException;
}
