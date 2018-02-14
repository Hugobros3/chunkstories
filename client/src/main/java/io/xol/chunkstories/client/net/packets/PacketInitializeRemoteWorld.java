package io.xol.chunkstories.client.net.packets;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import io.xol.chunkstories.api.client.net.ClientPacketsProcessor;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.PacketReceptionContext;
import io.xol.chunkstories.api.util.concurrency.Fence;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.net.ClientPacketsProcessorImplementation;
import io.xol.chunkstories.net.packets.PacketSendWorldInfo;
import io.xol.chunkstories.world.WorldClientRemote;
import io.xol.chunkstories.world.WorldInfoImplementation;
import io.xol.chunkstories.world.WorldLoadingException;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PacketInitializeRemoteWorld extends PacketSendWorldInfo {

	public void process(PacketSender sender, DataInputStream in, PacketReceptionContext processor) throws IOException
	{
		//This is messy slow and all, but I rather this than dealing with the insane bullshit of UTF-8/16 wizzardry required to bypass the convience of a BufferedReader
		//And I can be Unicode-correct so fancy pants bloggers don't get mad at me
		int size = in.readInt();
		byte[] vaChier = new byte[size];
		in.readFully(vaChier);
		
		ByteArrayInputStream bais = new ByteArrayInputStream(vaChier);
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(bais, "UTF-8"));
		info = new WorldInfoImplementation(reader);
		
		if (processor instanceof ClientPacketsProcessor)
		{
			ClientPacketsProcessor cpp = (ClientPacketsProcessor)processor;
			
			//Asks
			//ClientInterface client = cpp.getContext();
			
			Client client = (Client)cpp.getContext(); //TODO should we expose this to the interface ?
			Fence fence = client.getGameWindow().queueSynchronousTask(new Runnable()
			{
				@Override
				public void run()
				{
					WorldClientRemote world;
					try {
						world = new WorldClientRemote(client, info, ((ClientPacketsProcessorImplementation)cpp).getConnection());
						Client.getInstance().changeWorld(world);
					} catch (WorldLoadingException e) {
						client.exitToMainMenu(e.getMessage());
					}
				}
			});
			
			fence.traverse();
		}
	}
}
