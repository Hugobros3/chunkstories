package io.xol.chunkstories.core.entity;

import io.xol.chunkstories.api.entity.EntityVoxel;
import io.xol.chunkstories.entity.EntityImplementation;
import io.xol.chunkstories.renderer.lights.DefferedLight;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.engine.math.lalgb.Vector3f;
import io.xol.engine.model.RenderingContext;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class EntityVoxelTest extends EntityImplementation implements EntityVoxel
{
	public EntityVoxelTest(WorldImplementation w, double x, double y, double z)
	{
		super(w, x, y, z);
	}

	@Override
	public void render(RenderingContext context)
	{
		//System.out.println("k man" + getLocation());
		
		Vector3f pos = getLocation().castToSP();
		pos.x += 0.5f;
		pos.z += 0.5f;
		
		pos.y += 1.5f;
		context.addLight(new DefferedLight(new Vector3f(1.0f, 1.0f, 0.0f), pos, 15f));
	}
}
