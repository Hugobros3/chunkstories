package io.xol.chunkstories.world.biomes;

import io.xol.chunkstories.world.generator.structures.GenerableStructure;
import io.xol.chunkstories.world.generator.util.SeededRandomNumberTranslator;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class DesertBiome extends Biome
{

	public DesertBiome(String name, float humidity, float temperature,
			float minHeight, float maxHeight, float size)
	{
		super(name, humidity, temperature, minHeight, maxHeight, size);
	}

	@Override
	public int getTopTile(int y)
	{
		return 0;
	}

	@Override
	public int getGroundTile(int height, int y)
	{
		return 12;
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
		/*
		 * if(height > 138) { if(rnd.getRandomChanceForChunkPlusModifier(x*32,
		 * z*32, x*32+z) > 998 && rnd.getRandomChanceForChunkPlusModifier(x*32,
		 * z*32, z*32+x+5) > 599) return 1; }
		 */
		return null;
	}

}
