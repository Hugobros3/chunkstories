package io.xol.chunkstories.item;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.entity.inventory.CSFSerializable;
import io.xol.chunkstories.entity.inventory.Inventory;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ItemPile implements CSFSerializable
{
	public int amount = 1;
	public Item item;
	
	public Inventory inventory;
	public int x,y;
	
	public ItemData data = null;
	
	public ItemPile(Item item)
	{
		this.item = item;
		this.data = item.getItemData();
		item.onCreate(this);
	}
	
	public ItemPile(Item item, DataInputStream stream)  throws IOException
	{
		this.item = item;
		this.data = item.getItemData();
		load(stream);
	}

	public String getTextureName()
	{
		return item.getTextureName();
	}

	public Item getItem()
	{
		return item;
	}

	@Override
	public void load(DataInputStream stream) throws IOException
	{
		this.amount = stream.readInt();
		item.load(this, stream);
	}

	@Override
	public void save(DataOutputStream stream) throws IOException
	{
		stream.writeInt(amount);
		item.save(this, stream);
	}

	/**
	 * Try to move an item to another slot
	 * @param inventory2 new slot's inventory
	 * @param x2
	 * @param y2
	 * @return
	 */
	public ItemPile moveTo(Inventory inventory2, int x2, int y2)
	{
		inventory.setItemPileAt(this.x, this.y, null);
		if(inventory2.canPlaceItemAt(x2, y2, this))
		{
			ItemPile nextSelection = inventory.getItem(x2, y2);
			inventory2.setItemPileAt(x2, y2, this);
			return nextSelection;
		}
		else
			inventory.setItemPileAt(this.x, this.y, this);
		return this;
	}
}
