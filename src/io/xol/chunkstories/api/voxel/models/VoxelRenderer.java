package io.xol.chunkstories.api.voxel.models;

import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.renderer.VoxelContext;
import io.xol.chunkstories.renderer.chunks.VoxelBaker;

public interface VoxelRenderer
{
	public int renderInto(VoxelBaker renderByteBuffer, VoxelContext info, Chunk chunk, int x, int y, int z);
}