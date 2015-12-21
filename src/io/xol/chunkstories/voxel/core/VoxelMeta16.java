package io.xol.chunkstories.voxel.core;

import io.xol.chunkstories.renderer.BlockRenderInfo;
import io.xol.chunkstories.voxel.Voxel;
import io.xol.chunkstories.voxel.VoxelTexture;
import io.xol.chunkstories.voxel.VoxelTextures;

public class VoxelMeta16 extends Voxel
{

	VoxelTexture colors[] = new VoxelTexture[16];

	public VoxelMeta16(int id, String name)
	{
		super(id, name);
		for (int i = 0; i < 16; i++)
			colors[i] = VoxelTextures.getVoxelTexture(name + "." + i);
	}

	public VoxelTexture getVoxelTexture(int side, BlockRenderInfo info) // 0 for top, 1 bot,
															// 2,3,4,5
															// north/south/east/west
	{
		int meta = info.getMetaData();
		// System.out.println("swag");
		return colors[meta];
	}
}
