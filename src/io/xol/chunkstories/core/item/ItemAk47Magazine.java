package io.xol.chunkstories.core.item;

import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.item.ItemType;
import io.xol.chunkstories.item.ItemPile;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ItemAk47Magazine extends Item
{
	public ItemAk47Magazine(ItemType type)
	{
		super(type);
	}

	@Override
	public String getTextureName(ItemPile pile)
	{
		// TODO Auto-generated method stub
		return "res/items/icons/ak47_magazine.png";
	}

}
