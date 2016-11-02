package io.xol.chunkstories.api.world;

import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.world.region.RegionImplementation;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public abstract class WorldGenerator
{
	protected World world;

	public void initialize(World world)
	{
		this.world = world;
	}

	/**
	 * Generates a chunk based on the folowing information
	 * @param cx coordinates of the chunk (world-space/32)
	 * @param cy
	 * @param cz
	 * @return a CubicChunk object
	 */
	//TODO sort this out
	public abstract Chunk generateChunk(RegionImplementation region, int cx, int cy, int cz);

	/**
	 * Returns the data {@link VoxelFormat} for summary generation
	 * @param x coordinates in world-space
	 * @param z
	 * @return
	 */
	public abstract int getTopDataAt(int x, int z);

	/**
	 * Returns the initial height for summary generation
	 * @param x
	 * @param z
	 * @return
	 */
	public abstract int getHeightAt(int x, int z);
}
