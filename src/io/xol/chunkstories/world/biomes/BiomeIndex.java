package io.xol.chunkstories.world.biomes;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class BiomeIndex
{
	public static Biome[] biomes = new Biome[512];
	public static int biomes_count = 0;
	public static int biomesIndexPrecision = 32;
	public static int[][][] biomesIndex = new int[biomesIndexPrecision][biomesIndexPrecision][biomesIndexPrecision];
	static
	{
		biomes[0] = new PlainsBiome("Plains", 0.5f, 0.45f, 0, 1024, 1f);
		// biomes[1] = new DesertBiome("Desert",0.2f,1f,0,1024,1f);
		biomes[1] = new ForestBiome("Forest", 0.8f, 0.55f, 0, 1024, 1.0f);
		System.out.println(biomes_count + " biomes initilized.");
		for (int a = 0; a < biomesIndexPrecision; a++)
			for (int b = 0; b < biomesIndexPrecision; b++)
				for (int c = 0; c < biomesIndexPrecision; c++)
				{
					biomesIndex[a][b][c] = matchBiome((a * 1f)
							/ biomesIndexPrecision, (b * 1f)
							/ biomesIndexPrecision, c * 32);
				}
		System.out.println("Biome index lookup initialized");
	}

	static int bv = 0;
	static int a = 0;

	private static int matchBiome(float humid, float temp, int height)
	{
		float distance = 0;
		Biome biome = biomes[0];
		for (Biome b : biomes)
		{
			if (b != null)
			{
				if (b.minHeight <= height && b.maxHeight >= height)
				{
					float nd = Math.abs(Math.abs(humid - b.humidity)
							* Math.abs(humid - b.humidity)
							+ Math.abs(temp - b.temperature)
							* Math.abs(temp - b.temperature));
					nd = nd / b.sizeMult;
					a++;
					if (nd > distance)
					{
						distance = nd;
						biome = b;
						bv++;
					}
				}
			}
		}
		// System.out.println("o ! humid:"+humid+"temp:"+temp+"a"+a+"bv"+bv);

		return biome.id;
	}

	public static int clamp(float in, int a, int b)
	{
		int i = (int) in;
		if (i < a)
			i = a;
		else if (i > b)
			i = b;
		return i;
	}

	public static Biome getBiomeFor(float humid, float temp, int height)
	{
		int h = clamp(humid * biomesIndexPrecision, 0, biomesIndexPrecision - 1);
		int t = clamp(temp * biomesIndexPrecision, 0, biomesIndexPrecision - 1);
		int hh = clamp(height * biomesIndexPrecision, 0,
				biomesIndexPrecision - 1);

		// System.out.println("humid:"+h+"temp:"+t+"hh:"+hh);
		// return biomes[0];
		return biomes[biomesIndex[h][t][hh]];
	}
}
