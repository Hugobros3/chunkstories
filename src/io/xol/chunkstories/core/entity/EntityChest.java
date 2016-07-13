package io.xol.chunkstories.core.entity;

import io.xol.chunkstories.api.entity.EntityInventory;
import io.xol.chunkstories.api.entity.EntityVoxel;
import io.xol.chunkstories.api.entity.interfaces.EntityWithInventory;
import io.xol.chunkstories.core.entity.components.EntityComponentPublicInventory;
import io.xol.chunkstories.entity.EntityImplementation;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.engine.model.RenderingContext;

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
	public void render(RenderingContext context)
	{

		/*Vector3f pos = getLocation().castToSP();
		pos.x += 0.5f;
		pos.z += 0.5f;
		
		pos.y += 1.5f;
		context.addLight(new DefferedLight(new Vector3f(1.0f, 0.0f, 0.0f), pos, 500f));*/
	}

	@Override
	public EntityInventory getInventory()
	{
		return inventoryComponent;
	}

}
