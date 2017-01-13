package io.xol.engine.math.random;

import java.util.Random;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class SeededRandomNumberTranslator
{

	int[] premadeRandom = new int[10000];

	public SeededRandomNumberTranslator(String seed)
	{
		// Init p array based on see
		long lseed = 0;
		long z = 1;
		for (byte b : seed.getBytes())
		{
			lseed += b * z;
			z *= 10;
			z %= 1874818188L;
		}
		Random rnd = new Random(lseed);
		for (int i = 0; i < 10000; i++)
		{
			premadeRandom[i] = rnd.nextInt(1000);
		}
	}

	public int getRandomChanceForChunk(int cx, int cz)
	{
		int pointer = cx * 100 + cz + cx * (53 - cz) + 7 * (cx % 47) + cz * cz
				* 5 + ((cx / 14) % 5) * (cz + 9);
		pointer %= 10000;
		if (pointer < 0)
			pointer += 10000;
		return premadeRandom[pointer];
	}

	public int getRandomChanceForChunkPlusModifier(int cx, int cz, int i)
	{
		int pointer = cx * 100 + cz + cx * (53 - cz) + 7 * (cx % 47) + cz * cz
				* 5 + ((cx / 14) % 5) * (cz + 9) + i * 7 + i + cx * i + cz * i
				* 3 - 47 * i * i * (cx / 4);
		pointer %= 10000;
		if (pointer < 0)
			pointer += 10000;
		return premadeRandom[pointer];
	}
}
