package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.voxel.VoxelType;
import io.xol.chunkstories.api.voxel.models.VoxelModel;
import io.xol.chunkstories.api.world.VoxelContext;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class VoxelRail extends Voxel
{
	public VoxelRail(VoxelType type)
	{
		super(type);
	}

	@Override
	public VoxelModel getVoxelRenderer(VoxelContext info)
	{
		if(info.getSideId(VoxelSides.FRONT.ordinal()) == this.getId())
			return store.models().getVoxelModelByName("rails.alt");

		return store.models().getVoxelModelByName("rails.default");
	}
}
