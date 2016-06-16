package io.xol.chunkstories.world.biomes;

import io.xol.chunkstories.world.generator.structures.GenerableStructure;
import io.xol.chunkstories.world.generator.util.SeededRandomNumberTranslator;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public abstract class Biome
{
	// A biome is defined by it's name, it's humidity, it's height and it's
	// temperature.

	public Biome(String name, float humidity, float temperature,
			float minHeight, float maxHeight, float sizeMult)
	{
		this.name = name;
		this.humidity = humidity;
		this.temperature = temperature;
		this.minHeight = minHeight;
		this.maxHeight = maxHeight;
		this.sizeMult = sizeMult;
		id = BiomeIndex.biomes_count;
		BiomeIndex.biomes_count++;
	}

	public String name;
	public int id;

	float humidity;
	float temperature;
	float minHeight;
	float maxHeight;
	float sizeMult;

	public abstract int getTopTile(int y); // Over-ground, ie grass etc

	public abstract int getGroundTile(int height, int y); // Get ground tile

	public abstract int getFluidTile(); // Get water type

	public GenerableStructure getTreeType(SeededRandomNumberTranslator srnt,
			int x, int height, int z)
	{
		return null;
	}
}
