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
}
