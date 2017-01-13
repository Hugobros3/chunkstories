package io.xol.chunkstories.net.packets;

import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.client.Client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

//(c) 2015-2017 XolioWare Interactive
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
	public float overcastFactor;
	
	@Override
	public void send(PacketDestinator destinator, DataOutputStream out) throws IOException
	{
		out.writeLong(time);
		out.writeFloat(overcastFactor);
	}

	public void process(PacketSender sender, DataInputStream in, PacketsProcessor processor) throws IOException
	{
		read(in);
		process(processor);
	}
	
	public void read(DataInputStream in) throws IOException
	{
		time = in.readLong();
		overcastFactor = in.readFloat();
	}

	public void process(PacketsProcessor processor)
	{
		//System.out.println("Got time packet");
		if(Client.world instanceof WorldClient)
		{
			Client.world.setTime(time);
			Client.world.setWeather(overcastFactor);;
		}
	}

}
