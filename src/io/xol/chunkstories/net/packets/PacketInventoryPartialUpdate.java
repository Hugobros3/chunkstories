package io.xol.chunkstories.net.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.entity.Inventory;
import io.xol.chunkstories.api.exceptions.PacketProcessingException;
import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.PacketSynchPrepared;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.item.ItemTypes;
import io.xol.chunkstories.net.InventoryTranslator;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PacketInventoryPartialUpdate extends PacketSynchPrepared
{
	private Inventory inventory;
	private int slotx, sloty;
	private ItemPile newItemPile;

	public PacketInventoryPartialUpdate()
	{
		
	}
	
	public PacketInventoryPartialUpdate(Inventory inventory, int slotx, int sloty, ItemPile newItemPile)
	{
		this.inventory = inventory;
		this.slotx = slotx;
		this.sloty = sloty;
		this.newItemPile = newItemPile;
	}

	@Override
	public void sendIntoBuffer(PacketDestinator destinator, DataOutputStream out) throws IOException
	{
		InventoryTranslator.writeInventoryHandle(out, inventory);
		
		out.writeInt(slotx);
		out.writeInt(sloty);
		
		if(newItemPile == null)
			out.writeInt(0);
		else{
			out.writeInt(newItemPile.getItem().getID());
			newItemPile.saveCSF(out);
		}
	}

	@Override
	public void process(PacketSender sender, DataInputStream in, PacketsProcessor processor) throws IOException, PacketProcessingException
	{
		inventory = InventoryTranslator.obtainInventoryHandle(in, processor);
		
		int slotx = in.readInt();
		int sloty = in.readInt();
		
		int itemId = in.readInt();
		
		if(itemId != 0)
		{
			Item item = ItemTypes.getItemTypeById(itemId).newItem();
			newItemPile = new ItemPile(item, in);
		}
		else
			newItemPile = null;
		
		if(inventory != null)
			inventory.setItemPileAt(slotx, sloty, newItemPile);
	}

}
