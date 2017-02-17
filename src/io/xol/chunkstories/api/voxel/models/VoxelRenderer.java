package io.xol.chunkstories.api.voxel.models;

import io.xol.chunkstories.api.voxel.models.ChunkRenderer.ChunkRenderContext;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

import io.xol.chunkstories.api.world.VoxelContext;
import io.xol.chunkstories.api.world.chunk.Chunk;

/** Renders voxels of a chunk into a mesh */
public interface VoxelRenderer
{
	public int renderInto(ChunkRenderer chunkRenderer, ChunkRenderContext bakingContext, Chunk chunk, VoxelContext voxelInformations);
}