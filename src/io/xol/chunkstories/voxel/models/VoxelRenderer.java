package io.xol.chunkstories.voxel.models;

import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.renderer.VoxelContext;
import io.xol.chunkstories.renderer.chunks.VoxelBaker;

public interface VoxelRenderer
{

	int renderInto(VoxelBaker renderByteBuffer, VoxelContext info, Chunk chunk, int x, int y, int z);

}