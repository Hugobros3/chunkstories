package io.xol.chunkstories.world;

import io.xol.chunkstories.api.sound.SoundManager;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.net.ClientToServerConnection;
import io.xol.chunkstories.net.packets.PacketsProcessor;
import io.xol.chunkstories.net.packets.PacketsProcessor.PendingSynchPacket;

import io.xol.chunkstories.world.io.IOTasksMultiplayerClient;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class WorldClientRemote extends WorldClientCommon implements WorldNetworked
{
	private ClientToServerConnection connection;
	private PacketsProcessor packetsProcessor;

	public WorldClientRemote(Client client, WorldInfo info, ClientToServerConnection connection)
	{
		super(client, info);

		this.connection = connection;
		this.packetsProcessor = connection.getPacketsProcessor();

		ioHandler = new IOTasksMultiplayerClient(this, packetsProcessor.getClientToServerConnection());
		ioHandler.start();
	}
	
	public ClientToServerConnection getConnection()
	{
		return connection;
	}

	@Override
	public SoundManager getSoundManager()
	{
		return Client.getInstance().getSoundManager();
	}
	
	public void destroy() {
		
		super.destroy();
		connection.close();
	}

	@Override
	public void processIncommingPackets()
	{
		//Accepts and processes synched packets

		entitiesLock.writeLock().lock();
		
		@SuppressWarnings("unused")
		int packetsThisTick = 0;
		PendingSynchPacket packet = packetsProcessor.getPendingSynchPacket();
		while (packet != null)
		{
			//System.out.println(packet);
			packet.process(this.getConnection(), packetsProcessor);
			packet = packetsProcessor.getPendingSynchPacket();
			packetsThisTick++;
		}

		entitiesLock.writeLock().unlock();
		//if(packetsThisTick > 0)
		//	System.out.println(packetsThisTick+"packets for "+this.entities.size()+ " entities");
	}
}
