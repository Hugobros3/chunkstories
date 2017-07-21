package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelType;
import io.xol.chunkstories.api.world.VoxelContext;
import io.xol.chunkstories.core.voxel.renderers.VoxelWaterRenderer;
import io.xol.chunkstories.voxel.VoxelsStore;
import io.xol.chunkstories.voxel.models.VoxelModelLoaded;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class VoxelLiquid extends Voxel
{
	VoxelModelLoaded surface;
	VoxelModelLoaded inside;

	public VoxelLiquid(VoxelType type)
	{
		super(type);
		inside = new VoxelWaterRenderer(store.models().getVoxelModelByName("water.inside"));
		surface = new VoxelWaterRenderer(store.models().getVoxelModelByName("water.surface"));
	}

	@Override
	public VoxelModelLoaded getVoxelRenderer(VoxelContext info)
	{
		int data = info.getSideId(4);
		if(!VoxelsStore.get().getVoxelById(data).getType().isLiquid())
			return surface;
		else return inside;
	}
}
