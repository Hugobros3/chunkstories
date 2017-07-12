package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.exceptions.IllegalBlockModificationException;
import io.xol.chunkstories.api.physics.CollisionBox;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelLogic;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.voxel.VoxelType;
import io.xol.chunkstories.api.voxel.models.VoxelModel;
import io.xol.chunkstories.api.world.VoxelContext;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.core.entity.EntityPlayer;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class VoxelStairs extends Voxel implements VoxelLogic
{
	VoxelModel[] models = new VoxelModel[8];

	public VoxelStairs(VoxelType type)
	{
		super(type);
		for (int i = 0; i < 8; i++)
			models[i] = store.models().getVoxelModelByName("stairs.m" + i);
	}

	@Override
	public VoxelModel getVoxelRenderer(VoxelContext info)
	{
		int meta = info.getMetaData();
		return models[meta % 8];
	}

	@Override
	public CollisionBox[] getCollisionBoxes(VoxelContext info)
	{
		int meta = VoxelFormat.meta(info.getData());
		CollisionBox[] boxes = new CollisionBox[2];
		boxes[0] = new CollisionBox(1, 0.5, 1);//.translate(0.5, -1, 0.5);
		switch (meta % 4)
		{
		case 0:
			boxes[1] = new CollisionBox(0.5, 0.5, 1.0).translate(0.5, -0.0, 0.0);
			break;
		case 1:
			boxes[1] = new CollisionBox(0.5, 0.5, 1.0).translate(0.0, -0.0, 0.0);
			break;
		case 2:
			boxes[1] = new CollisionBox(1.0, 0.5, 0.5).translate(0.0, -0.0, 0.5);
			break;
		case 3:
			boxes[1] = new CollisionBox(1.0, 0.5, 0.5).translate(0.0, -0.0, 0.0);
			break;
		default:
			boxes[1] = new CollisionBox(0.5, 0.5, 1.0).translate(0.5, -0.0, 0.25);
			break;
		}
		
		if (meta / 4 == 0)
		{
			boxes[0].translate(0.0, 0.0, 0.0);
			boxes[1].translate(0.0, 0.5, 0.0);
		}
		else
		{
			boxes[0].translate(0.0, 0.5, 0.0);
			boxes[1].translate(0.0, 0.0, 0.0);
		}

		return boxes;
	}

	@Override
	public int getLightLevelModifier(int dataFrom, int dataTo, VoxelSides side)
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
			double dx = loc.x() - (x + 0.5);
			double dz = loc.z() - (z + 0.5);

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
				if(((EntityPlayer)entity).getEntityRotationComponent().getVerticalRotation() < 0)
					stairsSide += 4;
			}
			
			voxelData = VoxelFormat.changeMeta(voxelData, stairsSide);
		}
		return voxelData;
	}

	@Override
	public void onRemove(World world, int x, int y, int z, int voxelData, Entity entity)
	{
		//System.out.println("on remove stairs");
	}
	
	@Override
	public int onModification(World world, int x, int y, int z, int voxelData, Entity entity) throws IllegalBlockModificationException
	{
		throw new IllegalBlockModificationException("Stairs can't be changed direction");
	}
}
