package io.xol.chunkstories.core.generator;

import io.xol.chunkstories.api.world.WorldGenerator;
import io.xol.chunkstories.world.chunk.CubicChunk;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class BlankWorldGenerator extends WorldGenerator
{

	@Override
	public CubicChunk generateChunk(int cx, int cy, int cz)
	{
		CubicChunk c = new CubicChunk(world, cx, cy, cz);
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
