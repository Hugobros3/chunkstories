package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.Content;
import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityVoxel;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.voxel.VoxelEntity;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.core.entity.voxel.EntityVoxelTest;
import io.xol.chunkstories.world.WorldImplementation;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class VoxelEntityTest extends VoxelEntity
{

	public VoxelEntityTest(Content.Voxels store, int id, String name)
	{
		super(store, id, name);
	}

	@Override
	public boolean handleInteraction(Entity entity, Location voxelLocation, Input input, int voxelData)
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
