package io.xol.chunkstories.core.entity.voxel;

import io.xol.chunkstories.api.entity.EntityBase;
import io.xol.chunkstories.api.entity.EntityVoxel;
import io.xol.chunkstories.api.entity.interfaces.EntityWithInventory;
import io.xol.chunkstories.api.item.inventory.Inventory;
import io.xol.chunkstories.api.physics.CollisionBox;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.core.entity.components.EntityComponentPublicInventory;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class EntityChest extends EntityBase implements EntityWithInventory, EntityVoxel
{
	EntityComponentPublicInventory inventoryComponent;
	
	public EntityChest(World w, double x, double y, double z)
	{
		super(w, x, y, z);
		inventoryComponent = new EntityComponentPublicInventory(this, 10, 6);
	}
	
	@Override
	public CollisionBox getBoundingBox()
	{
		return new CollisionBox(1.0, 1.0, 1.0);
	}

	@Override
	public Inventory getInventory()
	{
		return inventoryComponent.getInventory();
	}

}
