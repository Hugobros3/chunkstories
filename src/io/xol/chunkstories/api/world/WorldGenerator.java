package io.xol.chunkstories.api.world;

import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.chunk.CubicChunk;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public abstract class WorldGenerator
{
	protected WorldImplementation world;

	public void initialize(WorldImplementation world)
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
	public abstract CubicChunk generateChunk(int cx, int cy, int cz);

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
