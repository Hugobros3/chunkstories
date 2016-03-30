package io.xol.chunkstories.voxel.core;

import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.BlockRenderInfo;
import io.xol.chunkstories.voxel.VoxelDefault;
import io.xol.chunkstories.voxel.models.VoxelModel;
import io.xol.chunkstories.voxel.models.VoxelModels;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class Voxel8Steps extends VoxelDefault
{
	VoxelModel[] steps = new VoxelModel[8];

	public Voxel8Steps(int id, String name)
	{
		super(id, name);
		for(int i = 0; i < 8; i++)
			steps[i] = VoxelModels.getVoxelModel("steps.m"+i);
		// System.out.println("kekzer");
	}

	@Override
	public VoxelModel getVoxelModel(BlockRenderInfo info)
	{
		return steps[info.getMetaData() % 8];
	}

	@Override
	public CollisionBox[] getCollisionBoxes(BlockRenderInfo info)
	{
		//System.out.println("kek");
		int meta = VoxelFormat.meta(info.data);
		CollisionBox box2 = new CollisionBox(1, (meta % 8 + 1) / 8f, 1);
		box2.translate(0.5, -0, 0.5);
		return new CollisionBox[] { box2 };
		//return super.getCollisionBoxes(data);
	}
}
