package io.xol.chunkstories.world.biomes;

import io.xol.chunkstories.world.generator.structures.GenerableStructure;
import io.xol.chunkstories.world.generator.structures.PineTree;
import io.xol.chunkstories.world.generator.util.SeededRandomNumberTranslator;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ForestBiome extends Biome
{

	public ForestBiome(String name, float humidity, float temperature,
			float minHeight, float maxHeight, float size)
	{
		super(name, humidity, temperature, minHeight, maxHeight, size);
	}

	@Override
	public int getTopTile(int y)
	{
		if (y > 130 && Math.random() > 0.8)
			return 65;
		return 0;
	}

	@Override
	public int getGroundTile(int height, int y)
	{
		if (y < 124)
			return 3;
		else if (y < 130)
			return 11;
		if (y == height)
			return 6;
		return 3;
	}

	@Override
	public int getFluidTile()
	{
		return 128;
	}

	@Override
	public GenerableStructure getTreeType(SeededRandomNumberTranslator rnd,
			int x, int height, int z)
	{
		if (height > 128 + 4)
		{
			if (rnd.getRandomChanceForChunkPlusModifier(x * 32, z * 32, x * 32
					+ z) > 988)
				return new PineTree(x, height, z, 8 + (int) (Math.random() * 5));
		}
		return null;
	}

}
