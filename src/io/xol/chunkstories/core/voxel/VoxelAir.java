package io.xol.chunkstories.core.voxel;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.BlockRenderInfo;
import io.xol.chunkstories.voxel.VoxelDefault;

public class VoxelAir extends VoxelDefault
{
	public VoxelAir(int id, String name)
	{
		super(id, name);
	}

	@Override
	public CollisionBox[] getCollisionBoxes(BlockRenderInfo info)
	{
		return new CollisionBox[] {};
	}
}
