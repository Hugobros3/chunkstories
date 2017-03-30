package io.xol.chunkstories.core.item.armor;

import io.xol.chunkstories.api.item.ItemType;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ItemArmorBoots extends ItemArmor
{
	public static final String[] bodyParts = {"boneFootR","boneFootL"};

	public ItemArmorBoots(ItemType type)
	{
		super(type);
	}

	@Override
	public String[] bodyPartsAffected()
	{
		return bodyParts;
	}

}
