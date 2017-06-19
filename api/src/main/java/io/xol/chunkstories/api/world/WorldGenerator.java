package io.xol.chunkstories.api.world;

import io.xol.chunkstories.api.Content.WorldGenerators.WorldGeneratorType;
import io.xol.chunkstories.api.world.chunk.Chunk;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public abstract class WorldGenerator
{
	protected final World world;
	protected final WorldGeneratorType type;

	public WorldGenerator(WorldGeneratorType type, World world)
	{
		this.world = world;
		this.type = type;
	}
	
	public WorldGeneratorType getType()
	{
		return type;
	}

	/**
	 * Generates a chunk based on the folowing information
	 * @param cx coordinates of the chunk (world-space/32)
	 * @param cy
	 * @param cz
	 */
	//TODO sort this out
	public abstract Chunk generateChunk(Chunk chunk);

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
