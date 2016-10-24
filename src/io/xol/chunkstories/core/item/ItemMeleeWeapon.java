package io.xol.chunkstories.core.item;

import io.xol.chunkstories.api.entity.DamageCause;
import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.item.ItemType;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ItemMeleeWeapon extends Item implements DamageCause
{
	final long swingDuration;
	final long hitTime;
	final double range;
	
	final float damage;
	
	long currentSwing = 0L;
	long cooldownLeft = 0L;
	
	public ItemMeleeWeapon(ItemType type)
	{
		super(type);
		
		swingDuration = Integer.parseInt(type.getProperty("swingDuration", "100"));
		hitTime = Integer.parseInt(type.getProperty("hitTime", "100"));

		range = Double.parseDouble(type.getProperty("range", "1"));
		damage = Float.parseFloat(type.getProperty("damage", "100"));
	}

}
