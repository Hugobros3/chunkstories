package io.xol.chunkstories.core.entity.voxel;

import io.xol.chunkstories.api.entity.EntityVoxel;
import io.xol.chunkstories.api.physics.CollisionBox;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.entity.EntityImplementation;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class EntityVoxelTest extends EntityImplementation implements EntityVoxel
{
	public EntityVoxelTest(World w, double x, double y, double z)
	{
		super(w, x, y, z);
	}
	
	@Override
	public CollisionBox getBoundingBox()
	{
		return new CollisionBox(1.0, 1.0, 1.0);
	}
}
