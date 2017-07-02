package io.xol.chunkstories.renderer.chunks;

import io.xol.chunkstories.renderer.chunks.ChunkMeshesBakerPool.ClientWorkerThread.ChunkMeshingBuffers;

public interface BakeChunkTaskExecutor {
	public ChunkMeshingBuffers getBuffers();
}
