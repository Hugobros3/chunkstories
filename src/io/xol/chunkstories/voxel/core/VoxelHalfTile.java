package io.xol.chunkstories.voxel.core;

import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.BlockRenderInfo;
import io.xol.chunkstories.voxel.Voxel;
import io.xol.chunkstories.voxel.VoxelFormat;
import io.xol.chunkstories.voxel.models.VoxelModel;
import io.xol.chunkstories.voxel.models.VoxelModels;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class VoxelHalfTile extends Voxel
{

	VoxelModel bot;
	VoxelModel top;

	public VoxelHalfTile(int id, String name)
	{
		super(id, name);
		bot = VoxelModels.getVoxelModel("halftile.bottom");
		top = VoxelModels.getVoxelModel("halftile.top");
		// System.out.println("kekzer");
	}

	boolean bottomOrTop(int meta)
	{
		// int meta = VoxelFormat.meta(data);
		return meta % 2 == 0;
	}

	public VoxelModel getVoxelModel(BlockRenderInfo info)
	{
		int meta = info.getMetaData();
		if (bottomOrTop(meta))
			return bot;
		return top;
	}

	public CollisionBox[] getCollisionBoxes(int data)
	{
		// System.out.println("kek");
		CollisionBox box2 = new CollisionBox(1, 0.5, 1);
		if (bottomOrTop(VoxelFormat.meta(data)))
			box2.translate(0.5, -1, 0.5);
		else
			box2.translate(0.5, -0.5, 0.5);
		return new CollisionBox[] { box2 };
	}
}
