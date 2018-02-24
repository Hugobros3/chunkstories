//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world;

import java.io.IOException;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;

import io.xol.chunkstories.api.content.OnlineContentTranslator;
import io.xol.chunkstories.api.exceptions.PacketProcessingException;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.net.PacketDefinition.PacketGenre;
import io.xol.chunkstories.api.net.PacketWorld;
import io.xol.chunkstories.api.net.RemoteServer;
import io.xol.chunkstories.api.sound.SoundManager;
import io.xol.chunkstories.api.world.WorldClientNetworkedRemote;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.net.ServerConnection;
import io.xol.chunkstories.net.LogicalPacketDatagram;
import io.xol.chunkstories.net.PacketDefinitionImpl;
import io.xol.chunkstories.net.PacketsContextCommon;
import io.xol.chunkstories.world.io.IOTasksMultiplayerClient;

public class WorldClientRemote extends WorldClientCommon implements WorldClientNetworkedRemote
{
	private final ServerConnection connection;
	private final PacketsContextCommon packetsProcessor;

	private final IOTasksMultiplayerClient mpIOHandler;
	
	private final OnlineContentTranslator translator;
	
	public WorldClientRemote(Client client, WorldInfoImplementation info, OnlineContentTranslator translator, ServerConnection connection) throws WorldLoadingException
	{
		super(client, info, translator);

		this.connection = connection;
		this.packetsProcessor = connection.getPacketsContext();

		this.translator = translator;
		
		mpIOHandler = new IOTasksMultiplayerClient(this);
		
		ioHandler = mpIOHandler;
		ioHandler.start();
	}
	
	public OnlineContentTranslator getContentTranslator() {
		return translator;
	}

	@Override
	public RemoteServer getRemoteServer() {
		return connection.getRemoteServer();
	}
	
	public ServerConnection getConnection()
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
		
		super.tick();
		
		getConnection().flush();
	}
	
	Deque<LogicalPacketDatagram> incommingDatagrams = new ConcurrentLinkedDeque<>();
	
	public void processIncommingPackets()
	{
		//Accepts and processes synched packets
		entitiesLock.writeLock().lock();
		
		@SuppressWarnings("unused")
		int packetsThisTick = 0;
		
		Iterator<LogicalPacketDatagram> i = incommingDatagrams.iterator();
		while (i.hasNext()) {
			LogicalPacketDatagram datagram = i.next();

			try {
				PacketDefinitionImpl definition = (PacketDefinitionImpl) datagram.packetDefinition; //this.getContentTranslator().getPacketForId(datagram.packetTypeId);
				Packet packet = definition.createNew(true, this);
				if(definition.getGenre() != PacketGenre.WORLD || !(packet instanceof PacketWorld)) {
					logger().error(definition + " isn't a PacketWorld");
				} else {
					PacketWorld packetWorld = (PacketWorld) packet;
					
					//packetsProcessor.getSender() is equivalent to getRemoteServer() here
					packetWorld.process(packetsProcessor.getInterlocutor(), datagram.getData(), packetsProcessor);
				}
			}
			catch(IOException | PacketProcessingException e) {
				logger().warn("Exception while processing datagram: "+e.getMessage());
			}
			
			datagram.dispose();
			
			i.remove();
			packetsThisTick++;
		}

		entitiesLock.writeLock().unlock();
	}
	
	public void queueDatagram(LogicalPacketDatagram datagram) {
		this.incommingDatagrams.add(datagram);
	}
	
	public IOTasksMultiplayerClient ioHandler()
	{
		return mpIOHandler;
	}
}
