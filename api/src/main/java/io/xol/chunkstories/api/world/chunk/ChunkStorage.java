package io.xol.chunkstories.api.world.chunk;

import io.xol.chunkstories.api.world.chunk.Chunk.ChunkVoxelContext;

public interface ChunkStorage
{
	/** Returns the chunk this is linked to. */
	public Chunk chunk();
	
	/** Returns the VoxelStorage object found at this location. If one such isn't found, it returns null if vsi == null, else it uses the passed vsi object to build a suitable VoxelStorage object, store it and return it. */
	public VoxelStorage obtainVoxelStorage(int x, int y, int z, VoxelStorageInitializer vsi);
	
	public interface VoxelStorageInitializer {
		public VoxelStorage initializeVoxelStorage(ChunkVoxelContext context);
	}
}
