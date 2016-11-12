package io.xol.chunkstories.net.packets;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.Inventory;
import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSynchPrepared;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.core.events.PlayerMoveItemEvent;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.item.ItemTypes;
import io.xol.chunkstories.net.InventoryTranslator;
import io.xol.chunkstories.server.Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PacketInventoryMoveItemPile extends PacketSynchPrepared
{
	public ItemPile itemPile;
	public Inventory from, to;
	public int oldX, oldY, newX, newY;
	public int amount;
	
	@Override
	public void sendIntoBuffer(PacketDestinator destinator, DataOutputStream out) throws IOException
	{
		//Describe the move
		out.writeInt(oldX);
		out.writeInt(oldY);
		out.writeInt(newX);
		out.writeInt(newY);
		
		out.writeInt(amount);
		
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
			out.writeInt(itemPile.getItem().getID());
			itemPile.saveCSF(out);
		}
	}

	public void process(PacketSender sender, DataInputStream in, PacketsProcessor processor) throws IOException
	{
		//read(in);
		//process(processor);
		
		oldX = in.readInt();
		oldY = in.readInt();
		newX = in.readInt();
		newY = in.readInt();
		
		amount = in.readInt();
		
		from = InventoryTranslator.obtainInventoryHandle(in, processor);
		
		to = InventoryTranslator.obtainInventoryHandle(in, processor);
		
		//If this pile is spawned from the void
		if(from == null)
		{
			Item item = ItemTypes.getItemTypeById(in.readInt()).newItem();
			itemPile = new ItemPile(item, in);
		}
		
		PlayerMoveItemEvent moveItemEvent = new PlayerMoveItemEvent(processor.getServerClient().getProfile(), this);
		Server.getInstance().getPluginsManager().fireEvent(moveItemEvent);
	}
}
