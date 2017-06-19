package io.xol.chunkstories.world.generator;

import io.xol.chunkstories.api.Content.WorldGenerators.WorldGeneratorType;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldGenerator;
import io.xol.chunkstories.api.world.chunk.Chunk;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class BlankWorldGenerator extends WorldGenerator
{

	public BlankWorldGenerator(WorldGeneratorType type, World world)
	{
		super(type, world);
	}

	@Override
	public Chunk generateChunk(Chunk c)
	{
		return c;
	}

	@Override
	public int getTopDataAt(int x, int y)
	{
		return 0;
	}

	@Override
	public int getHeightAt(int x, int z)
	{
		return 0;
	}
}
