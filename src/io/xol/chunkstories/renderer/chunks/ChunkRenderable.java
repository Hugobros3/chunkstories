package io.xol.chunkstories.renderer.chunks;

import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.ShadingType;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.renderer.chunks.ChunkRenderDataHolder.RenderLodLevel;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface ChunkRenderable extends Chunk
{
	public void markForReRender();
	
	public boolean isMarkedForReRender();
	
	public void markRenderInProgress(boolean inProgress);
	
	public boolean isRenderAleadyInProgress();
	
	public void redrawChunk();
	
	//Implementation details, you don't need to worry about those
	
	public ChunkRenderDataHolder getChunkRenderData();
	
	public int renderPass(RenderingInterface renderingInterface, RenderLodLevel renderLodLevel, ShadingType shadingType);
}
