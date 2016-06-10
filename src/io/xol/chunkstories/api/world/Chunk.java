package io.xol.chunkstories.api.world;

import io.xol.chunkstories.api.voxel.VoxelFormat;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface Chunk
{

	/**
	 * Get the data contained in this chunk as full 32-bit data format ( see {@link VoxelFormat})
	 * The coordinates are internally modified to map to the chunk, meaning you can access it both with world coordinates or 0-31 in-chunk coordinates
	 * Just don't give it negatives
	 * @param x 
	 * @param y
	 * @param z
	 * @return the data contained in this chunk as full 32-bit data format ( see {@link VoxelFormat})
	 */
	public int getDataAt(int x, int y, int z);

	/**
	 * Sets the data contained in this chunk as full 32-bit data format ( see {@link VoxelFormat})
	 * It will also trigger lightning and such updates
	 * The coordinates are internally modified to map to the chunk, meaning you can access it both with world coordinates or 0-31 in-chunk coordinates
	 * Just don't give it negatives
	 * @param x
	 * @param y
	 * @param z
	 * @param data
	 */
	public void setDataAtWithUpdates(int x, int y, int z, int data);

	/**
	 * Sets the data contained in this chunk as full 32-bit data format ( see {@link VoxelFormat})
	 * The coordinates are internally modified to map to the chunk, meaning you can access it both with world coordinates or 0-31 in-chunk coordinates
	 * Just don't give it negatives
	 * @param x
	 * @param y
	 * @param z
	 * @param data
	 */
	public void setDataAtWithoutUpdates(int x, int y, int z, int data);
	
	/**
	 * Marks the chunk to be re-rendered
	 * @param priority May put the chunk on the top of the list of chunks to render
	 */
	void markDirty(boolean priority);

	/**
	 * Recomputes and propagates all lights within the chunk
	 * @param adjacent If set to true, the adjacent faces of the 6 adjacents chunks's data will be took in charge
	 */
	public void bakeVoxelLightning(boolean adjacent);
	
	public int getSunLight(int x, int y, int z);
	
	public int getBlockLight(int x, int y, int z);
	
	public boolean isAirChunk();
	
	public void setSunLight(int x, int y, int z, int level);
	
	public void setBlockLight(int x, int y, int z, int level);

}