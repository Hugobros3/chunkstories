package io.xol.chunkstories.api.world;

import java.util.Deque;

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
	int getDataAt(int x, int y, int z);

	/**
	 * Sets the data contained in this chunk as full 32-bit data format ( see {@link VoxelFormat})
	 * The coordinates are internally modified to map to the chunk, meaning you can access it both with world coordinates or 0-31 in-chunk coordinates
	 * Just don't give it negatives
	 * @param x
	 * @param y
	 * @param z
	 * @param data
	 */
	void setDataAt(int x, int y, int z, int data);

	/**
	 * Marks the chunk to be re-rendered
	 * @param priority May put the chunk on the top of the list of chunks to render
	 */
	void markDirty(boolean priority);

	/**
	 * Internal class for lightning updates. You may want to call it yourself, in wich case it uses two Deque as buffers.
	 * The reason you have to supply it yourself is optimization, you generally need to avoid creating and destroying those buffers too often when you have hundreds of
	 * chunks to recalculate.
	 * @param adjacent
	 * @param blockSources
	 * @param sunSources
	 */
	void doLightning(boolean adjacent, Deque<Integer> blockSources, Deque<Integer> sunSources);

}