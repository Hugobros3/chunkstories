package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.renderer.VoxelContext;
import io.xol.chunkstories.voxel.VoxelDefault;
import io.xol.chunkstories.voxel.models.VoxelModel;
import io.xol.chunkstories.voxel.models.VoxelModels;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class VoxelRail extends VoxelDefault
{
	public VoxelRail(int id, String name)
	{
		super(id, name);
	}

	@Override
	public VoxelModel getVoxelRenderer(VoxelContext info)
	{
		if(info.getSideId(VoxelSides.FRONT.ordinal()) == this.voxelID)
			return VoxelModels.getVoxelModel("rails.alt");

		return VoxelModels.getVoxelModel("rails.default");
	}
}
