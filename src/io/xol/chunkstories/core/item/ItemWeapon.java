package io.xol.chunkstories.core.item;

import io.xol.chunkstories.api.entity.DamageCause;
import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.item.ItemType;

public abstract class ItemWeapon extends Item implements DamageCause
{
	public ItemWeapon(ItemType type)
	{
		super(type);
	}

}
