package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityVoxel;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.voxel.VoxelEntity;
import io.xol.chunkstories.api.voxel.VoxelType;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.World.WorldVoxelContext;
import io.xol.chunkstories.core.entity.voxel.EntityVoxelTest;
import io.xol.chunkstories.world.WorldImplementation;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class VoxelEntityTest extends VoxelEntity
{

	public VoxelEntityTest(VoxelType type)
	{
		super(type);
	}

	@Override
	public boolean handleInteraction(Entity entity, WorldVoxelContext voxelContext, Input input)
	{
		// TODO Auto-generated method stub
		// System.out.println("kekossorus");
		
		return false;
	}

	@Override
	protected EntityVoxel createVoxelEntity(World world, int x, int y, int z)
	{
		return new EntityVoxelTest((WorldImplementation) world, x, y, z);
	}

}
