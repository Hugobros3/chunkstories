package io.xol.chunkstories.core.entity;

import java.util.Iterator;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.interfaces.EntityWithInventory;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.core.item.armor.ItemArmor;
import io.xol.chunkstories.entity.components.EntityComponentInventory;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class EntityArmorInventory extends EntityComponentInventory {

	public EntityArmorInventory(EntityWithInventory holder, int width, int height)
	{
		super(holder, width, height);

		this.actualInventory = new OnlyArmorEntityInventory(width, height);
	}
	
	class OnlyArmorEntityInventory extends EntityInventory {

		public OnlyArmorEntityInventory(int width, int height)
		{
			super(width, height);
		}
		
		@Override
		public boolean canPlaceItemAt(int x, int y, ItemPile itemPile)
		{
			if(itemPile.getItem() instanceof ItemArmor)
			{
				return super.canPlaceItemAt(x, y, itemPile);
			}
			return false;
		}
		
		@Override
		public String getInventoryName()
		{
			return "Armor";
		}
		
		public float getDamageMultiplier(String bodyPartName) {
			
			float multiplier = 1.0f;
			
			Iterator<ItemPile> i = this.iterator();
			while(i.hasNext())
			{
				ItemPile p = i.next();
				ItemArmor a = (ItemArmor)p.getItem();

				String[] bpa = a.bodyPartsAffected();
				if(bodyPartName == null && bpa == null)
					multiplier *= a.damageMultiplier(bodyPartName);
				else if(bodyPartName != null){
					if(bpa == null)
						multiplier *= a.damageMultiplier(bodyPartName);
					else
					{
						for(int j = 0; j < bpa.length; j++)
						{
							if(bpa[j].equals(bodyPartName))
							{
								multiplier *= a.damageMultiplier(bodyPartName);
								break;
							}
						}
					}
				}
				//Of BPN == null & BPA != null, we don't do shit
			}
			
			return multiplier;
		}
	}
	
	interface EntityWithArmor extends Entity {
		
		public EntityArmorInventory getArmor();
	}
}