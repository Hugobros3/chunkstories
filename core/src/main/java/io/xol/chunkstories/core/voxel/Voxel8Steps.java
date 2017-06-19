package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.physics.CollisionBox;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelType;
import io.xol.chunkstories.api.voxel.models.VoxelModel;
import io.xol.chunkstories.api.world.VoxelContext;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class Voxel8Steps extends Voxel
{
	VoxelModel[] steps = new VoxelModel[8];

	public Voxel8Steps(VoxelType type)
	{
		super(type);
		for(int i = 0; i < 8; i++)
			steps[i] = getType().store().models().getVoxelModelByName("steps.m"+i);
	}

	@Override
	public VoxelModel getVoxelRenderer(VoxelContext info)
	{
		return steps[info.getMetaData() % 8];
	}

	@Override
	public CollisionBox[] getCollisionBoxes(VoxelContext info)
	{
		//System.out.println("kek");
		int meta = VoxelFormat.meta(info.getData());
		CollisionBox box2 = new CollisionBox(1, (meta % 8 + 1) / 8f, 1);
		//box2.translate(0.5, -0, 0.5);
		return new CollisionBox[] { box2 };
		//return super.getCollisionBoxes(data);
	}
}
