package io.xol.chunkstories.api.world;

import io.xol.chunkstories.api.voxel.VoxelFormat;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface Chunk
{
	public World getWorld();
	
	public Region getRegion();

	public int getChunkX();
	
	public int getChunkY();
	
	public int getChunkZ();
	
	/**
	 * Get the data contained in this chunk as full 32-bit data format ( see {@link VoxelFormat})
	 * The coordinates are internally modified to map to the chunk, meaning you can access it both with world coordinates or 0-31 in-chunk coordinates
	 * Just don't give it negatives
	 * @return the data contained in this chunk as full 32-bit data format ( see {@link VoxelFormat})
	 */
	public int getVoxelData(int x, int y, int z);

	/**
	 * Sets the data contained in this chunk as full 32-bit data format ( see {@link VoxelFormat})
	 * It will also trigger lightning and such updates
	 * The coordinates are internally modified to map to the chunk, meaning you can access it both with world coordinates or 0-31 in-chunk coordinates
	 * Just don't give it negatives
	 * @param data The raw block data, see {@link VoxelFormat}
	 */
	public void setVoxelDataWithUpdates(int x, int y, int z, int data);

	/**
	 * Sets the data contained in this chunk as full 32-bit data format ( see {@link VoxelFormat})
	 * The coordinates are internally modified to map to the chunk, meaning you can access it both with world coordinates or 0-31 in-chunk coordinates
	 * Just don't give it negatives
	 * @param data The raw block data, see {@link VoxelFormat}
	 */
	public void setVoxelDataWithoutUpdates(int x, int y, int z, int data);

	/**
	 * Recomputes and propagates all lights within the chunk
	 * @param considerAdjacentChunks If set to true, the adjacent faces of the 6 adjacents chunks's data will be took in charge
	 */
	public void bakeVoxelLightning(boolean considerAdjacentChunks);
	
	public boolean needsLightningUpdates();
	
	public void markInNeedForLightningUpdate();
	
	public int getSunLight(int x, int y, int z);
	
	public int getBlockLight(int x, int y, int z);
	
	public void setSunLight(int x, int y, int z, int level);
	
	public void setBlockLight(int x, int y, int z, int level);
	
	public boolean isAirChunk();

	public void destroy();

}