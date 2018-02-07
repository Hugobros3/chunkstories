package io.xol.chunkstories.client.net;

import java.io.DataInputStream;
import java.io.IOException;

import io.xol.chunkstories.api.client.net.ClientPacketsProcessor;
import io.xol.chunkstories.api.exceptions.net.IllegalPacketException;
import io.xol.chunkstories.api.exceptions.net.UnknowPacketException;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.net.PacketsProcessorCommon;
import io.xol.chunkstories.world.WorldClientRemote;

public class ClientPacketsProcessorImplementation extends PacketsProcessorCommon implements ClientPacketsProcessor {

	final Client client;
	final ClientConnectionToServer clientConnection;
	
	public ClientPacketsProcessorImplementation(Client gameContext, ClientConnectionToServer clientConnection) {
		super(gameContext);
		
		this.client = gameContext;
		this.clientConnection = clientConnection;
	}
	
	public ClientConnectionToServer getConnection() {
		return clientConnection;
	}

	@Override
	public WorldClient getWorld() {
		return client.getWorld();
	}

	@Override
	public Client getContext() {
		return client;
	}

	@Override
	public Player getPlayer() {
		return client.getPlayer();
	}

	@Override
	public Packet getPacket_(DataInputStream in) throws IOException, UnknowPacketException, IllegalPacketException
	{
		/*while(true)
		{
			Packet packet = super.getPacket(in);
			
			if (packet instanceof PacketWorldStreaming)
			{
				PacketWorldStreaming pws = (PacketWorldStreaming)packet;
				
				//Pre-read it before doing anything with it
				pws.process(getConnection(), in, this);
				
				WorldClient world = getWorld();
				if(world instanceof WorldClientRemote)
				{
					WorldClientRemote wcr = (WorldClientRemote)world;
					wcr.ioHandler().handlePacketWorldStreaming(pws);
				}
				else
					throw new IllegalPacketException(pws) {

					private static final long serialVersionUID = -3549167260495141819L;

					@Override
					public String getMessage()
					{
						return "Illegal packet received : This is a local server, not a remote client world and we shouldn't receive WorldStreamingPackets ( "+packet.getClass().getName()+" )";
					}
				};
				
				continue;
			}
			
			return packet;
		}*/
	}
}
