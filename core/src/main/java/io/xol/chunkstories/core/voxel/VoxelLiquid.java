package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelType;
import io.xol.chunkstories.api.world.VoxelContext;
import io.xol.chunkstories.core.voxel.renderers.VoxelWaterRenderer;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class VoxelLiquid extends Voxel
{
	VoxelWaterRenderer surface;
	VoxelWaterRenderer inside;

	public VoxelLiquid(VoxelType type)
	{
		super(type);
		inside = new VoxelWaterRenderer(store.models().getVoxelModelByName("water.inside"));
		surface = new VoxelWaterRenderer(store.models().getVoxelModelByName("water.surface"));
	}

	@Override
	public VoxelWaterRenderer getVoxelRenderer(VoxelContext info)
	{
		//Return the surface only if the top block isn't liquid
		int data = info.getSideId(4);
		if(!store.getVoxelById(data).getType().isLiquid())
			return surface;
		else return inside;
	}
}
