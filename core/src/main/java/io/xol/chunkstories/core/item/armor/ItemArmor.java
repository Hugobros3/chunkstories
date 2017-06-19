package io.xol.chunkstories.core.item.armor;

import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.item.ItemType;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class ItemArmor extends Item
{
	private final float mutiplier;
	private final String overlay;
	
	public ItemArmor(ItemType type)
	{
		super(type);
		
		mutiplier = Float.parseFloat(type.resolveProperty("damageMultiplier", "0.5"));
		overlay = type.resolveProperty("overlay", "notexture");
	}
	
	/** Returns either null (and affects the entire holder) or a list of body parts it cares about */
	public abstract String[] bodyPartsAffected();
	
	/** Returns the multiplier, optional bodyPartName (might be null, handled that case) for entities that support it */
	public float damageMultiplier(String bodyPartName)
	{	
		return mutiplier;
	}
	
	public String getOverlayTextureName() {
		return overlay;
	}

}
