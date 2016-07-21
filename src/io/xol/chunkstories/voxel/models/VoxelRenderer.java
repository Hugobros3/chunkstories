package io.xol.chunkstories.voxel.models;

import io.xol.chunkstories.api.world.Chunk;
import io.xol.chunkstories.renderer.BlockRenderInfo;
import io.xol.chunkstories.renderer.chunks.VoxelBaker;

public interface VoxelRenderer
{

	int renderInto(VoxelBaker renderByteBuffer, BlockRenderInfo info, Chunk chunk, int x, int y, int z);

}