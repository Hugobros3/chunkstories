package io.xol.chunkstories.core.voxel.renderers;

import io.xol.chunkstories.api.voxel.models.ChunkRenderer;
import io.xol.chunkstories.api.voxel.models.VoxelBakerHighPoly;
import io.xol.chunkstories.api.voxel.models.VoxelModel;
import io.xol.chunkstories.api.voxel.models.VoxelRenderer;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.LodLevel;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.ShadingType;
import io.xol.chunkstories.api.voxel.models.ChunkRenderer.ChunkRenderContext;
import io.xol.chunkstories.api.world.VoxelContext;
import io.xol.chunkstories.api.world.chunk.Chunk;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class VoxelWaterRenderer implements VoxelRenderer
{
	VoxelModel model;
	
	public VoxelWaterRenderer(VoxelModel model)
	{
		this.model = model;
	}

	@Override
	public int renderInto(ChunkRenderer chunkRenderer, ChunkRenderContext bakingContext, Chunk chunk, VoxelContext info)
	{
		VoxelBakerHighPoly renderByteBuffer = chunkRenderer.getHighpolyBakerFor(LodLevel.ANY, ShadingType.LIQUIDS);
		
		return model.renderInto(renderByteBuffer, bakingContext, info, chunk, info.getX(), info.getY(), info.getZ());
	}
}
