package io.xol.chunkstories.renderer;

import io.xol.chunkstories.renderer.chunks.ChunkRenderable;

public interface ChunkMeshesBaker
{
	public void requestChunkRender(ChunkRenderable chunk);

	public void destroy();
}
