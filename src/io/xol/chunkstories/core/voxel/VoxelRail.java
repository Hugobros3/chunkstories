package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.Content;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.renderer.VoxelContext;
import io.xol.chunkstories.voxel.VoxelDefault;
import io.xol.chunkstories.voxel.models.VoxelModel;
import io.xol.chunkstories.voxel.models.VoxelModelsStore;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class VoxelRail extends VoxelDefault
{
	public VoxelRail(Content.Voxels store, int id, String name)
	{
		super(store, id, name);
	}

	@Override
	public VoxelModel getVoxelRenderer(VoxelContext info)
	{
		if(info.getSideId(VoxelSides.FRONT.ordinal()) == this.voxelID)
			return store.models().getVoxelModelByName("rails.alt");

		return store.models().getVoxelModelByName("rails.default");
	}
}
