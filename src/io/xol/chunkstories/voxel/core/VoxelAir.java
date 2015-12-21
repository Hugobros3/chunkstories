package io.xol.chunkstories.voxel.core;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.voxel.Voxel;

public class VoxelAir extends Voxel
{

	public VoxelAir(int id, String name)
	{
		super(id, name);
	}

	public CollisionBox[] getCollisionBoxes()
	{
		return null;
	}
}
