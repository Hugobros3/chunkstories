package io.xol.chunkstories.net.packets;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityInventory;
import io.xol.chunkstories.api.entity.EntityWithInventory;
import io.xol.chunkstories.api.events.core.PlayerMoveItemEvent;
import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.item.ItemsList;
import io.xol.chunkstories.server.Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PacketInventoryMoveItemPile extends Packet
{
	public PacketInventoryMoveItemPile(boolean client)
	{
		super(client);
	}

	@Override
	public void send(PacketDestinator destinator, DataOutputStream out) throws IOException
	{
		//Describe the move
		out.writeInt(oldX);
		out.writeInt(oldY);
		out.writeInt(newX);
		out.writeInt(newY);
		//Describe the inventories
		//A lone itemPile or a holderless inventory is described by 0x00
		if(from == null || from.getHolder() == null)
			out.writeByte(0x00);
		else if(from.getHolder() instanceof Entity)
		{
			out.writeByte(0x01);
			out.writeLong(((Entity)from.getHolder()).getUUID());
		}
		if(to == null || to.getHolder() == null)
			out.writeByte(0x00);
		else if(to.getHolder() instanceof Entity)
		{
			out.writeByte(0x01);
			out.writeLong(((Entity)to.getHolder()).getUUID());
			//System.out.println("writing uuid"+((Entity)to.holder).getUUID());
		}
		//Describe the itemPile if we are trying to spawn an item from nowhere
		if(from == null || from.getHolder() == null)
		{
			out.writeInt(itemPile.item.getID());
			itemPile.saveCSF(out);
		}
	}

	public void process(DataInputStream in, PacketsProcessor processor) throws IOException
	{
		read(in);
		process(processor);
	}
	
	public void read(DataInputStream in) throws IOException
	{
		oldX = in.readInt();
		oldY = in.readInt();
		newX = in.readInt();
		newY = in.readInt();
		
		holderTypeFrom = in.readByte();
		if(holderTypeFrom == 0x01)
			eIdFrom = in.readLong();
		
		holderTypeTo = in.readByte();
		if(holderTypeTo == 0x01)
			eIdTo = in.readLong();
		
		if(holderTypeFrom == 0x00)
		{
			Item item = ItemsList.get(in.readInt());
			itemPile = new ItemPile(item, in);
		}
	}
	
	public ItemPile itemPile;
	public EntityInventory from, to;
	public int oldX, oldY, newX, newY;

	byte holderTypeFrom, holderTypeTo;
	
	long eIdFrom, eIdTo;
	
	public void process(PacketsProcessor processor)
	{
		System.out.println(eIdFrom+"="+eIdTo +"   " + holderTypeFrom+":"+holderTypeTo);
		
		if(holderTypeFrom == 0x01)
		{
			EntityWithInventory entity = (EntityWithInventory) processor.getWorld().getEntityByUUID(eIdFrom);
			if(entity != null)
				from = entity.getInventory();
		}
		if(holderTypeTo == 0x01)
		{
			EntityWithInventory entity = (EntityWithInventory) processor.getWorld().getEntityByUUID(eIdTo);
			if(entity != null)
				to = entity.getInventory();
		}
		
		PlayerMoveItemEvent moveItemEvent = new PlayerMoveItemEvent(processor.getServerClient().getProfile(), this);
		Server.getInstance().getPluginsManager().fireEvent(moveItemEvent);
	}

}
