package io.xol.chunkstories.net.packets;

import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.client.Client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * The server just tells the time
 * @author gobrosse
 *
 */
public class PacketTime extends Packet
{
	public long time;
	
	public PacketTime(boolean client)
	{
		super(client);
	}

	@Override
	public void send(PacketDestinator destinator, DataOutputStream out) throws IOException
	{
		out.writeLong(time);
	}

	public void process(DataInputStream in, PacketsProcessor processor) throws IOException
	{
		read(in);
		process(processor);
	}
	
	public void read(DataInputStream in) throws IOException
	{
		time = in.readLong();
	}

	public void process(PacketsProcessor processor)
	{
		//System.out.println("Got time packet");
		if(Client.world instanceof WorldClient)
		{
			Client.world.setTime(time);
		}
	}

}
