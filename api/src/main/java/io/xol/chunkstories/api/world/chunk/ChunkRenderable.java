package io.xol.chunkstories.api.world.chunk;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface ChunkRenderable extends Chunk
{
	public void markForReRender();
	
	public boolean isMarkedForReRender();
	
	public void markRenderInProgress(boolean inProgress);
	
	public boolean isRenderAleadyInProgress();
}
