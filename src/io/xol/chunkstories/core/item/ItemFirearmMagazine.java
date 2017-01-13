package io.xol.chunkstories.core.item;

import java.util.HashSet;
import java.util.Set;

import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.item.ItemType;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ItemFirearmMagazine extends Item
{
	Set<String> supportedWeaponsSet = new HashSet<String>();
	
	public ItemFirearmMagazine(ItemType type)
	{
		super(type);
		
		for(String s : type.getProperty("forWeapon", "").split(","))
			supportedWeaponsSet.add(s);
	}
	
	public boolean isSuitableFor(ItemFirearm item)
	{
		return supportedWeaponsSet.contains(item.getInternalName());
	}

}
