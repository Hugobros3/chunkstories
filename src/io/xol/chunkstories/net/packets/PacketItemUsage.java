package io.xol.chunkstories.net.packets;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.events.core.PlayerSelectItemEvent;
import io.xol.chunkstories.server.Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * The client uses that to signal to the server wich item it wants to use and when it does make use of it.
 * @author gobrosse
 *
 */
public class PacketItemUsage extends Packet
{

	public enum ItemUsage {
		SELECT,
		USE,
	}
	
	public ItemUsage usage;
	public byte complementInfo;
	
	public PacketItemUsage(boolean client)
	{
		super(client);
	}

	@Override
	public void send(DataOutputStream out) throws IOException
	{
		out.writeByte((byte)usage.ordinal());
		out.writeByte(complementInfo);
	}

	@Override
	public void read(DataInputStream in) throws IOException
	{
		usage = ItemUsage.values()[in.readByte()];
		complementInfo = in.readByte();
	}

	@Override
	public void process(PacketsProcessor processor)
	{
		//System.out.println("Got packet select/use item");
		
		Entity clientEntity = processor.getServerClient().profile.getControlledEntity();
		if(clientEntity == null)
		{
			System.out.println("Client entity is null, it can't select an item !");
			return;
		}
		
		if(usage == ItemUsage.SELECT)
		{
			PlayerSelectItemEvent selectItemEvent = new PlayerSelectItemEvent(processor.getServerClient().profile, clientEntity, complementInfo);
			Server.getInstance().getPluginsManager().fireEvent(selectItemEvent);
		}
		else if(usage == ItemUsage.USE)
		{
			
		}
	}

}
