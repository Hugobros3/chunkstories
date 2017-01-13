package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.Content;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.VoxelContext;
import io.xol.chunkstories.voxel.VoxelDefault;
import io.xol.chunkstories.voxel.models.VoxelModel;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class Voxel8Steps extends VoxelDefault
{
	VoxelModel[] steps = new VoxelModel[8];

	public Voxel8Steps(Content.Voxels store, int id, String name)
	{
		super(store, id, name);
		for(int i = 0; i < 8; i++)
			steps[i] = store.models().getVoxelModelByName("steps.m"+i);
		// System.out.println("kekzer");
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
		int meta = VoxelFormat.meta(info.data);
		CollisionBox box2 = new CollisionBox(1, (meta % 8 + 1) / 8f, 1);
		box2.translate(0.5, -0, 0.5);
		return new CollisionBox[] { box2 };
		//return super.getCollisionBoxes(data);
	}
}
