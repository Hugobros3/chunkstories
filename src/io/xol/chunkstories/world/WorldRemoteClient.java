package io.xol.chunkstories.world;

import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.net.packets.PacketsProcessor;
import io.xol.chunkstories.net.packets.PacketsProcessor.PendingSynchPacket;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import io.xol.chunkstories.world.io.IOTasksMultiplayerClient;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class WorldRemoteClient extends WorldImplementation implements WorldClient, WorldNetworked
{
	private PacketsProcessor packetsProcessor;
	
	public WorldRemoteClient(WorldInfo info, PacketsProcessor packetsProcessor)
	{
		super(info);
		
		this.packetsProcessor = packetsProcessor;
		
		ioHandler = new IOTasksMultiplayerClient(this);
		ioHandler.start();
		
	}

	@Override
	public Client getClient()
	{
		return Client.getInstance();
	}

	@Override
	public void processIncommingPackets()
	{
		PendingSynchPacket packet = packetsProcessor.getPendingSynchPacket();
		while(packet != null)
		{
			packet.process(Client.getInstance().getServerConnection(), packetsProcessor);
			packet = packetsProcessor.getPendingSynchPacket();
		}
	}
}
