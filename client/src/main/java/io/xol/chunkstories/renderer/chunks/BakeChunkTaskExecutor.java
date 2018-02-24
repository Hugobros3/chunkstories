//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.renderer.chunks;

import io.xol.chunkstories.renderer.chunks.ClientWorkerThread.ChunkMeshingBuffers;

public interface BakeChunkTaskExecutor {
	public ChunkMeshingBuffers getBuffers();
}
