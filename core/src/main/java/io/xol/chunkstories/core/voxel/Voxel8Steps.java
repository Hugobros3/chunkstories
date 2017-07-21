package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.physics.CollisionBox;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.voxel.VoxelType;
import io.xol.chunkstories.api.voxel.models.VoxelModel;
import io.xol.chunkstories.api.voxel.models.VoxelRenderer;
import io.xol.chunkstories.api.world.VoxelContext;
import io.xol.chunkstories.core.voxel.renderers.SmoothStepVoxelRenderer;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class Voxel8Steps extends Voxel
{
	VoxelModel[] steps = new VoxelModel[8];
	SmoothStepVoxelRenderer nextGen;

	public Voxel8Steps(VoxelType type)
	{
		super(type);
		for(int i = 0; i < 8; i++)
			steps[i] = getType().store().models().getVoxelModelByName("steps.m"+i);
		
		nextGen = new SmoothStepVoxelRenderer(this, steps);
	}
	
	@Override
	public VoxelRenderer getVoxelRenderer(VoxelContext info)
	{
		//return nextGen;
		return steps[info.getMetaData() % 8];
	}

	@Override
	public boolean isFaceOpaque(VoxelSides side, int data) {
		if(side == VoxelSides.BOTTOM)
			return true;
		if(side == VoxelSides.TOP)
			return true;
		
		return super.isFaceOpaque(side, data);
	}

	@Override
	public CollisionBox[] getCollisionBoxes(VoxelContext info)
	{
		int meta = VoxelFormat.meta(info.getData());
		CollisionBox box2 = new CollisionBox(1, (meta % 8 + 1) / 8f, 1);
		return new CollisionBox[] { box2 };
	}
}
