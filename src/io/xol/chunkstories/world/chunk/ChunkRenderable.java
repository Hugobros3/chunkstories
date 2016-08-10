package io.xol.chunkstories.world.chunk;

import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.renderer.chunks.ChunkRenderData;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface ChunkRenderable extends Chunk
{
	public void markForReRender();
	
	public boolean isMarkedForReRender();
	
	public void markRenderInProgress(boolean inProgress);
	
	public boolean isRenderAleadyInProgress();
	
	public void destroyRenderData();
	
	//Implementation details, you don't need to worry about those
	
	public void setChunkRenderData(ChunkRenderData chunkRenderData);
	
	public ChunkRenderData getChunkRenderData();
}
