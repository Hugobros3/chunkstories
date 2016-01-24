package io.xol.chunkstories.voxel.core;

import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.BlockRenderInfo;
import io.xol.chunkstories.voxel.VoxelDefault;
import io.xol.chunkstories.voxel.models.VoxelModel;
import io.xol.chunkstories.voxel.models.VoxelModels;

public class VoxelStairs extends VoxelDefault
{

	VoxelModel[] models = new VoxelModel[4];

	public VoxelStairs(int id, String name)
	{
		super(id, name);
		for (int i = 0; i < 4; i++)
			models[i] = VoxelModels.getVoxelModel("stairs.m" + i);
	}

	public VoxelModel getVoxelModel(BlockRenderInfo info)
	{
		int meta = info.getMetaData();
		return models[meta % 4];
	}

	public CollisionBox[] getCollisionBoxes(BlockRenderInfo info)
	{
		int meta = VoxelFormat.meta(info.data);
		// System.out.println("kek"+meta);
		CollisionBox[] boxes = new CollisionBox[2];
		boxes[0] = new CollisionBox(1, 0.5, 1).translate(0.5, -1, 0.5);
		switch (meta % 4)
		{
		case 0:
			boxes[1] = new CollisionBox(0.5, 0.5, 1.0).translate(0.75, -0.5, 0.5);
			break;
		case 1:
			boxes[1] = new CollisionBox(0.5, 0.5, 1.0).translate(0.25, -0.5, 0.5);
			break;
		case 2:
			boxes[1] = new CollisionBox(1.0, 0.5, 0.5).translate(0.5, -0.5, 0.75);
			break;
		case 3:
			boxes[1] = new CollisionBox(1.0, 0.5, 0.5).translate(0.5, -0.5, 0.25);
			break;
		default:
			boxes[1] = new CollisionBox(0.5, 0.5, 1.0).translate(0.5, -0.5, 0.25);
			break;
		}

		return boxes;

		/*
		 * CollisionBox box = new CollisionBox(1,0.5,1); if(bottomOrTop(data))
		 * box.translate(0.5, -1, 0.5); else box.translate(0.5, -0.5, 0.5);
		 * return new CollisionBox[] { box };
		 */
		// return super.getCollisionBoxes(data);
	}
}
