package io.xol.chunkstories.world;

import io.xol.chunkstories.api.net.PacketWorld;
import io.xol.chunkstories.api.net.PacketWorldStreaming;
import io.xol.chunkstories.api.net.RemoteServer;
import io.xol.chunkstories.api.sound.SoundManager;
import io.xol.chunkstories.api.world.WorldClientNetworkedRemote;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.net.ClientConnectionToServer;
import io.xol.chunkstories.net.PacketsProcessorCommon;
import io.xol.chunkstories.world.io.IOTasksMultiplayerClient;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class WorldClientRemote extends WorldClientCommon implements WorldClientNetworkedRemote
{
	private ClientConnectionToServer connection;
	private PacketsProcessorCommon packetsProcessor;

	private IOTasksMultiplayerClient mpIOHandler;
	
	public WorldClientRemote(Client client, WorldInfoImplementation info,  ClientConnectionToServer connection) throws WorldLoadingException
	{
		super(client, info);

		this.connection = connection;
		this.packetsProcessor = connection.getPacketsProcessor();

		mpIOHandler = new IOTasksMultiplayerClient(this);
		
		ioHandler = mpIOHandler;
		ioHandler.start();
	}

	@Override
	public RemoteServer getRemoteServer() {
		return connection;
	}
	
	public ClientConnectionToServer getConnection()
	{
		return connection;
	}

	@Override
	public SoundManager getSoundManager()
	{
		return Client.getInstance().getSoundManager();
	}
	
	@Override
	public void destroy() {
		
		super.destroy();
		connection.close();
	}

	@Override
	public void tick() {
		
		//Specific MP stuff
		processIncommingPackets();
		getConnection().flush();
		
		super.tick();
	}
	
	@Override
	public void processIncommingPackets()
	{
		//Accepts and processes synched packets
		
		throw new UnsupportedOperationException("TODO");
		/*entitiesLock.writeLock().lock();
		
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

		entitiesLock.writeLock().unlock();*/
		//if(packetsThisTick > 0)
		//	System.out.println(packetsThisTick+"packets for "+this.entities.size()+ " entities");
	}
	
	public IOTasksMultiplayerClient ioHandler()
	{
		return mpIOHandler;
	}

	@Override
	public void queueWorldPacket(PacketWorld packet) {
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public void queueWorldStreamingPacket(PacketWorldStreaming packet) {
		throw new UnsupportedOperationException("TODO");
	}
}
