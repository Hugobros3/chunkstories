package io.xol.chunkstories.item;

import io.xol.chunkstories.entity.inventory.Inventory;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ItemPile
{
	public int amount = 1;
	public Item item;
	
	public Inventory inventory;
	public int x,y;
	
	public ItemPile(Item item)
	{
		this.item = item;
	}
	
	public String getTextureName()
	{
		return item.getTextureName();
	}
}
