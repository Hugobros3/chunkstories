package io.xol.chunkstories.core.item;

import io.xol.chunkstories.api.entity.DamageCause;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityDamageCause;
import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.item.ItemType;
import io.xol.chunkstories.api.item.inventory.Inventory;
import io.xol.chunkstories.api.item.inventory.InventoryHolder;
import io.xol.chunkstories.api.item.inventory.ItemPile;

public class ItemWeapon extends Item
{

	public ItemWeapon(ItemType type)
	{
		super(type);
	}

	public DamageCause pileAsDamageCause(ItemPile pile)
	{
		Inventory inventory = pile.getInventory();
		if(inventory != null)
		{
			InventoryHolder holder = inventory.getHolder();
			if(holder != null && holder instanceof Entity) {

				Entity entity = (Entity)holder;
				return new EntityDamageCause() {

					@Override
					public String getName()
					{
						return ItemWeapon.this.getName() + " #{weildby} " + entity.toString();
					}

					@Override
					public Entity getResponsibleEntity()
					{
						return entity;
					}
					
				};
			}
		}
		
		return new DamageCause() {

			@Override
			public String getName()
			{
				return ItemWeapon.this.getName();
			}
			
		};
	}

}
