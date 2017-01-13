package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.Content;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import io.xol.chunkstories.renderer.VoxelContext;
import io.xol.chunkstories.voxel.VoxelDefault;
import io.xol.chunkstories.voxel.models.VoxelModel;

public class VoxelLadder extends VoxelDefault implements VoxelClimbable
{
	VoxelModel[] models = new VoxelModel[4];

	public VoxelLadder(Content.Voxels store, int id, String name)
	{
		super(store,  id, name);
		for (int i = 0; i < 4; i++)
			models[i] = store.models().getVoxelModelByName("dekal.m" + i);
	}

	@Override
	public VoxelModel getVoxelRenderer(VoxelContext info)
	{
		int meta = info.getMetaData();
		if(meta == 2)
			return models[2];
		else if(meta == 3)
			return models[3];
		else if(meta == 4)
			return models[0];
		else if(meta == 5)
			return models[1];
		return models[0];
	}
}
