package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelType;
import io.xol.chunkstories.renderer.VoxelContext;
import io.xol.chunkstories.voxel.VoxelsStore;
import io.xol.chunkstories.voxel.models.VoxelModel;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class VoxelLiquid extends Voxel
{
	VoxelModel surface;
	VoxelModel inside;

	public VoxelLiquid(VoxelType type)
	{
		super(type);
		inside = new VoxelWaterRenderer(store.models().getVoxelModelByName("water.inside"));
		surface = new VoxelWaterRenderer(store.models().getVoxelModelByName("water.surface"));
	}

	@Override
	public VoxelModel getVoxelRenderer(VoxelContext info)
	{
		int data = info.getSideId(4);
		if(!VoxelsStore.get().getVoxelById(data).getType().isLiquid())
			return surface;
		else return inside;
	}
}
