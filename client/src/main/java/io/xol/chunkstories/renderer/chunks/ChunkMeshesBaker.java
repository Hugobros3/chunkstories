package io.xol.chunkstories.renderer.chunks;

import io.xol.chunkstories.api.rendering.world.ChunkRenderable;

public interface ChunkMeshesBaker
{
	public void requestChunkRender(ChunkRenderable chunk);

	public void destroy();
}
