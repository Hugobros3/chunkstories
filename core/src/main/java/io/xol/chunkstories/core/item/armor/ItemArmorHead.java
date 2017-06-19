package io.xol.chunkstories.core.item.armor;

import io.xol.chunkstories.api.item.ItemType;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ItemArmorHead extends ItemArmor
{
	public static final String[] bodyParts = {"boneHead"};

	public ItemArmorHead(ItemType type)
	{
		super(type);
	}

	@Override
	public String[] bodyPartsAffected()
	{
		return bodyParts;
	}

}
