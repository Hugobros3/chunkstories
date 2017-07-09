package io.xol.chunkstories.core.entity.voxel;

import io.xol.chunkstories.api.entity.EntityBase;
import io.xol.chunkstories.api.entity.EntityType;
import io.xol.chunkstories.api.entity.EntityVoxel;
import io.xol.chunkstories.api.physics.CollisionBox;
import io.xol.chunkstories.api.world.World;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class EntityVoxelTest extends EntityBase implements EntityVoxel
{
	public EntityVoxelTest(EntityType t, World w, double x, double y, double z)
	{
		super(t, w, x, y, z);
	}
	
	@Override
	public CollisionBox getBoundingBox()
	{
		return new CollisionBox(1.0, 1.0, 1.0);
	}
}
