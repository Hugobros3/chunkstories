package io.xol.chunkstories.renderer;

import io.xol.chunkstories.api.rendering.world.ChunkRenderable;

public interface ChunkMeshesBaker
{
	public void requestChunkRender(ChunkRenderable chunk);

	public void destroy();
}
