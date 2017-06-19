package io.xol.chunkstories.renderer;

import io.xol.chunkstories.api.world.chunk.ChunkRenderable;

public interface ChunkMeshesBaker
{
	public void requestChunkRender(ChunkRenderable chunk);

	public void destroy();
}
