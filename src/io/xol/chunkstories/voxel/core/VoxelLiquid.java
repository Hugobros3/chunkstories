package io.xol.chunkstories.voxel.core;

import io.xol.chunkstories.renderer.BlockRenderInfo;
import io.xol.chunkstories.voxel.VoxelDefault;
import io.xol.chunkstories.voxel.VoxelTypes;
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
		inside = VoxelModels.getVoxelModel("water.inside");
		surface = VoxelModels.getVoxelModel("water.surface");
	}

	@Override
	public VoxelModel getVoxelModel(BlockRenderInfo info)
	{
		int data = info.getSideId(4);
		if(!VoxelTypes.get(data).isVoxelLiquid())
			return surface;
		else return inside;
	}
}
