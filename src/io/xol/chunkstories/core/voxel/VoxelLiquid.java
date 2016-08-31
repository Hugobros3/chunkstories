package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.renderer.VoxelContext;
import io.xol.chunkstories.voxel.VoxelDefault;
import io.xol.chunkstories.voxel.Voxels;
import io.xol.chunkstories.voxel.models.VoxelModel;
import io.xol.chunkstories.voxel.models.VoxelModels;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class VoxelLiquid extends VoxelDefault
{
	VoxelModel surface;
	VoxelModel inside;

	public VoxelLiquid(int id, String name)
	{
		super(id, name);
		inside = new VoxelWaterRenderer(VoxelModels.getVoxelModel("water.inside"));
		surface = new VoxelWaterRenderer(VoxelModels.getVoxelModel("water.surface"));
	}

	@Override
	public VoxelModel getVoxelRenderer(VoxelContext info)
	{
		int data = info.getSideId(4);
		if(!Voxels.get(data).isVoxelLiquid())
			return surface;
		else return inside;
	}
}
