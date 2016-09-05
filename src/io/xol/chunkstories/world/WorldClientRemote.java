package io.xol.chunkstories.world;

import io.xol.chunkstories.api.sound.SoundManager;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.net.packets.PacketsProcessor;
import io.xol.chunkstories.net.packets.PacketsProcessor.PendingSynchPacket;

import io.xol.chunkstories.world.io.IOTasksMultiplayerClient;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class WorldClientRemote extends WorldClientCommon implements WorldNetworked
{
	private PacketsProcessor packetsProcessor;

	public WorldClientRemote(WorldInfo info, PacketsProcessor packetsProcessor)
	{
		super(info);

		this.packetsProcessor = packetsProcessor;

		ioHandler = new IOTasksMultiplayerClient(this);
		ioHandler.start();

	}

	@Override
	public SoundManager getSoundManager()
	{
		return Client.getInstance().getSoundManager();
	}

	@Override
	public void processIncommingPackets()
	{
		//Accepts and processes synched packets
		int packetsThisTick = 0;
		PendingSynchPacket packet = packetsProcessor.getPendingSynchPacket();
		while (packet != null)
		{
			//System.out.println(packet);
			packet.process(this.getClient().getServerConnection(), packetsProcessor);
			packet = packetsProcessor.getPendingSynchPacket();
			packetsThisTick++;
		}
		//if(packetsThisTick > 0)
		//	System.out.println(packetsThisTick+"packets for "+this.entities.size()+ " entities");
	}
}
