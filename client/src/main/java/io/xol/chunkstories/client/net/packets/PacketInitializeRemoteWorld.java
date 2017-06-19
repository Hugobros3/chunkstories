package io.xol.chunkstories.client.net.packets;

import java.io.DataInputStream;
import java.io.IOException;

import io.xol.chunkstories.api.client.net.ClientPacketsProcessor;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.PacketsProcessor;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.net.ClientPacketsProcessorImplementation;
import io.xol.chunkstories.net.packets.PacketSendWorldInfo;
import io.xol.chunkstories.world.WorldClientRemote;
import io.xol.chunkstories.world.WorldInfoImplementation;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PacketInitializeRemoteWorld extends PacketSendWorldInfo {

	public void process(PacketSender sender, DataInputStream in, PacketsProcessor processor) throws IOException
	{
		short length = in.readShort();

		byte[] bytes = new byte[length];

		in.read(bytes, 0, length);

		char[] chars2 = new char[length / 2];
		for (int i = 0; i < chars2.length; i++)
			chars2[i] = (char) ((bytes[i * 2] << 8) + (bytes[i * 2 + 1] & 0xFF));

		info = new WorldInfoImplementation(new String(chars2), "");
		
		if (processor instanceof ClientPacketsProcessor)
		{
			ClientPacketsProcessor cpp = (ClientPacketsProcessor)processor;
			
			//Asks
			//ClientInterface client = cpp.getContext();
			
			Client client = (Client)cpp.getContext(); //TODO should we expose this to the interface ?
			client.getGameWindow().queueTask(new Runnable()
			{
				@Override
				public void run()
				{
					WorldClientRemote world = new WorldClientRemote(client, info, ((ClientPacketsProcessorImplementation)cpp).getConnection());
					Client.getInstance().changeWorld(world);
				}
			});
		}
	}
}
