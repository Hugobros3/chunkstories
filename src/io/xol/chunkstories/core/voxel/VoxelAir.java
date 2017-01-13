package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.Content;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.VoxelContext;
import io.xol.chunkstories.voxel.VoxelDefault;

public class VoxelAir extends VoxelDefault
{
	public VoxelAir(Content.Voxels store, int id, String name)
	{
		super(store, id, name);
	}

	@Override
	public CollisionBox[] getCollisionBoxes(VoxelContext info)
	{
		return new CollisionBox[] {};
	}
}
