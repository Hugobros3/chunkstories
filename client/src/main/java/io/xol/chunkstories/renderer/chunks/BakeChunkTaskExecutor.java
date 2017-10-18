package io.xol.chunkstories.renderer.chunks;

import io.xol.chunkstories.renderer.chunks.ClientWorkerThread.ChunkMeshingBuffers;

public interface BakeChunkTaskExecutor {
	public ChunkMeshingBuffers getBuffers();
}
