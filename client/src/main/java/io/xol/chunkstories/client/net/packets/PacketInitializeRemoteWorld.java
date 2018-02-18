package io.xol.chunkstories.client.net.packets;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.content.OnlineContentTranslator;
import io.xol.chunkstories.api.net.PacketReceptionContext;
import io.xol.chunkstories.api.util.concurrency.Fence;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.net.ClientPacketsContext;
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
		String initializationString = in.readUTF();
		
		ByteArrayInputStream bais = new ByteArrayInputStream(initializationString.getBytes("UTF-8"));
		BufferedReader reader = new BufferedReader(new InputStreamReader(bais, "UTF-8"));
		info = new WorldInfoImplementation(reader);
		
		if (processor instanceof ClientPacketsContext)
		{
			processor.logger().info("Received World initialization packet");
			ClientPacketsContext cpp = (ClientPacketsContext)processor;
			
			OnlineContentTranslator contentTranslator = cpp.getContentTranslator();
			if (contentTranslator == null) {
				processor.logger().error("Can't initialize a world without a ContentTranslator initialized first!");
				return;
			}
			
			Client client = (Client)cpp.getContext(); //TODO should we expose this to the interface ?
			Fence fence = client.getGameWindow().queueSynchronousTask(new Runnable()
			{
				@Override
				public void run()
				{
					WorldClientRemote world;
					try {
						world = new WorldClientRemote(client, info, contentTranslator, cpp.getConnection());
						client.changeWorld(world);
						
						cpp.getConnection().handleSystemRequest("world/ok");
					} catch (WorldLoadingException e) {
						client.exitToMainMenu(e.getMessage());
					}
				}
			});
			
			fence.traverse();
		}
	}
}
