package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelType;
import io.xol.chunkstories.api.world.VoxelContext;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import io.xol.chunkstories.physics.CollisionBox;

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
