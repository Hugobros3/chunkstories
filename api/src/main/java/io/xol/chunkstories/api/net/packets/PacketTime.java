package io.xol.chunkstories.api.net.packets;

import io.xol.chunkstories.api.client.net.ClientPacketsProcessor;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.PacketsProcessor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * The server just tells the time
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
		time = in.readLong();
		overcastFactor = in.readFloat();
		if(processor instanceof ClientPacketsProcessor)
		{
			ClientPacketsProcessor cpp = (ClientPacketsProcessor)processor;
			cpp.getWorld().setTime(time);
			cpp.getWorld().setWeather(overcastFactor);
		}
	}

}
