package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.physics.CollisionBox;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelType;
import io.xol.chunkstories.api.world.VoxelContext;

public class VoxelAir extends Voxel {

	public VoxelAir(VoxelType type)
	{
		super(type);
	}

	@Override
	public CollisionBox[] getCollisionBoxes(VoxelContext info)
	{
		return new CollisionBox[] {};
	}
}
