package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.Content;
import io.xol.chunkstories.renderer.VoxelContext;
import io.xol.chunkstories.voxel.VoxelDefault;
import io.xol.chunkstories.voxel.VoxelsStore;
import io.xol.chunkstories.voxel.models.VoxelModel;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class VoxelLiquid extends VoxelDefault
{
	VoxelModel surface;
	VoxelModel inside;

	public VoxelLiquid(Content.Voxels store, int id, String name)
	{
		super(store, id, name);
		inside = new VoxelWaterRenderer(store.models().getVoxelModelByName("water.inside"));
		surface = new VoxelWaterRenderer(store.models().getVoxelModelByName("water.surface"));
	}

	@Override
	public VoxelModel getVoxelRenderer(VoxelContext info)
	{
		int data = info.getSideId(4);
		if(!VoxelsStore.get().getVoxelById(data).isVoxelLiquid())
			return surface;
		else return inside;
	}
}
