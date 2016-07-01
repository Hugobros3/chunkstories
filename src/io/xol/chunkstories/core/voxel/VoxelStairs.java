package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelLogic;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.core.entity.EntityPlayer;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.BlockRenderInfo;
import io.xol.chunkstories.voxel.VoxelDefault;
import io.xol.chunkstories.voxel.models.VoxelModel;
import io.xol.chunkstories.voxel.models.VoxelModels;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class VoxelStairs extends VoxelDefault implements VoxelLogic
{
	VoxelModel[] models = new VoxelModel[8];

	public VoxelStairs(int id, String name)
	{
		super(id, name);
		for (int i = 0; i < 8; i++)
			models[i] = VoxelModels.getVoxelModel("stairs.m" + i);
	}

	@Override
	public VoxelModel getVoxelModel(BlockRenderInfo info)
	{
		int meta = info.getMetaData();
		return models[meta % 8];
	}

	@Override
	public CollisionBox[] getCollisionBoxes(BlockRenderInfo info)
	{
		int meta = VoxelFormat.meta(info.data);
		// System.out.println("kek"+meta);
		CollisionBox[] boxes = new CollisionBox[2];
		boxes[0] = new CollisionBox(1, 0.5, 1);//.translate(0.5, -1, 0.5);
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
		if (meta / 4 == 0)
		{
			boxes[0].translate(0.5, -0, 0.5);
			boxes[1].translate(0.0, +1.0, 0.0);
		}
		else
		{
			boxes[0].translate(0.5, +0.5, 0.5);
			boxes[1].translate(0.0, +0.5, 0.0);
		}

		return boxes;

		/*
		 * CollisionBox box = new CollisionBox(1,0.5,1); if(bottomOrTop(data))
		 * box.translate(0.5, -1, 0.5); else box.translate(0.5, -0.5, 0.5);
		 * return new CollisionBox[] { box };
		 */
		// return super.getCollisionBoxes(data);
	}

	@Override
	public int getLightLevelModifier(int dataFrom, int dataTo, int side)
	{
		return super.getLightLevelModifier(dataFrom, dataTo, side);
	}

	@Override
	public int onPlace(World world, int x, int y, int z, int voxelData, Entity entity)
	{
		// id+dir of slope
		// 0LEFT x-
		// 1RIGHT x+
		// 2BACK z-
		// 3FRONT z+
		
		int stairsSide = 0;
		if (entity != null)
		{
			Location loc = entity.getLocation();
			double dx = loc.getX() - (x + 0.5);
			double dz = loc.getZ() - (z + 0.5);

			//System.out.println("dx: "+dx+" dz:" + dz);
			
			if (Math.abs(dx) > Math.abs(dz))
			{
				if(dx > 0)
					stairsSide = 1;
				else
					stairsSide = 0;
			}
			else
			{
				if(dz > 0)
					stairsSide = 3;
				else
					stairsSide = 2;
			}
			
			if(entity instanceof EntityPlayer)
			{
				if(((EntityPlayer)entity).getEntityRotationComponent().getRotV() < 0)
					stairsSide += 4;
			}
			
			System.out.println("tamerde"+VoxelFormat.meta(voxelData));
			
			voxelData = VoxelFormat.changeMeta(voxelData, stairsSide);
		}

		System.out.println("tamer"+VoxelFormat.meta(voxelData));

		//System.out.println("on place stairs"+stairsSide);
		return voxelData;
	}

	@Override
	public void onRemove(World world, int x, int y, int z, int voxelData, Entity entity)
	{
		//System.out.println("on remove stairs");
	}
}
