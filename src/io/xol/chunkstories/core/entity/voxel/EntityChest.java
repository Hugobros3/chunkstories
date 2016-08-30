package io.xol.chunkstories.core.entity.voxel;

import io.xol.chunkstories.api.entity.EntityInventory;
import io.xol.chunkstories.api.entity.EntityVoxel;
import io.xol.chunkstories.api.entity.interfaces.EntityWithInventory;
import io.xol.chunkstories.core.entity.components.EntityComponentPublicInventory;
import io.xol.chunkstories.entity.EntityImplementation;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.engine.graphics.RenderingContext;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class EntityChest extends EntityImplementation implements EntityWithInventory, EntityVoxel
{
	EntityComponentPublicInventory inventoryComponent;
	
	public EntityChest(WorldImplementation w, double x, double y, double z)
	{
		super(w, x, y, z);
		inventoryComponent = new EntityComponentPublicInventory(this, 10, 6);
	}

	@Override
	public EntityInventory getInventory()
	{
		return inventoryComponent;
	}

}
