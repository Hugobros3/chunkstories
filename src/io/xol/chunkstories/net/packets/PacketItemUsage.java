package io.xol.chunkstories.net.packets;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSender;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * The client uses that to signal to the server wich item it wants to use and when it does make use of it.
 */
public class PacketItemUsage extends Packet
{
	public enum ItemUsage {
		//SELECT,
		USE,
	}
	
	public ItemUsage usage;
	public byte complementInfo;

	@Override
	public void send(PacketDestinator destinator, DataOutputStream out) throws IOException
	{
		out.writeByte((byte)usage.ordinal());
		out.writeByte(complementInfo);
	}
	
	public void process(PacketSender sender, DataInputStream in, PacketsProcessor processor) throws IOException
	{
		read(in);
		process(processor);
	}
	
	public void read(DataInputStream in) throws IOException
	{
		usage = ItemUsage.values()[in.readByte()];
		complementInfo = in.readByte();
	}
	
	public void process(PacketsProcessor processor)
	{
		//System.out.println("Got packet select/use item");
		
		Entity clientEntity = processor.getServerClient().getProfile().getControlledEntity();
		if(clientEntity == null)
		{
			System.out.println("Client entity is null, it can't select an item !");
			return;
		}
		
		/*if(usage == ItemUsage.SELECT)
		{
			PlayerSelectItemEvent selectItemEvent = new PlayerSelectItemEvent(processor.getServerClient().getProfile(), (EntityWithInventory) clientEntity, complementInfo);
			Server.getInstance().getPluginsManager().fireEvent(selectItemEvent);
		}
		else */
		if(usage == ItemUsage.USE)
		{
			
		}
	}

}
