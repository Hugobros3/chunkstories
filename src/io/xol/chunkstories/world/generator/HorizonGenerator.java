package io.xol.chunkstories.world.generator;

import java.util.Random;

import io.xol.chunkstories.api.world.WorldGenerator;
import io.xol.chunkstories.world.CubicChunk;
import io.xol.chunkstories.world.World;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class HorizonGenerator extends WorldGenerator
{
	Random rnd = new Random();
	SeededSimplexNoiseGenerator ssng;
	
	public void initialize(World w)
	{
		super.initialize(w);
		ssng = new SeededSimplexNoiseGenerator(w.seed);
		ws = world.getSizeInChunks() * 32;
	}
	
	int ws;
	
	@Override
	public CubicChunk generateChunk(int cx, int cy, int cz)
	{
		rnd.setSeed(cx * 32 + cz + 48716148);
		
		CubicChunk c = new CubicChunk(world, cx, cy, cz);
		for(int x = 0; x < 32; x++)
			for(int z = 0; z < 32; z++)
			{
				//int v = getHeightAt(cx * 32 + x, cz * 32 + z);
				int v = world.chunkSummaries.getHeightAt(cx * 32 + x, cz * 32 + z);
				//int v = 250;
				int y = cy * 32;
				while(y < cy * 32 + 32 && y < v)
				{
					c.setDataAt(x, y, z, 1);
					y++;
				}
			}
		//c.setDataAt(0, 0, 0, 3);
		//c.setDataAt(0, 0, 1, 0);
		return c;
	}
	
	float fractalNoise(int x, int z, int octaves, float freq, float persistence)
	{
		float total = 0.0f;
		float maxAmplitude = 0.0f;
		float amplitude = 1.0f;
		for(int i = 0; i < octaves; i++)
		{
			total += ssng.looped_noise(x * freq, z * freq, ws) * amplitude;
			freq*=2.0f;
			maxAmplitude += amplitude;
			amplitude *= persistence;
		}
		return total / maxAmplitude;
	}
	
	float ridgedNoise(int x, int z, int octaves, float freq, float persistence)
	{
		float total = 0.0f;
		float maxAmplitude = 0.0f;
		float amplitude = 1.0f;
		freq *= ws / (64 * 32);
		for(int i = 0; i < octaves; i++)
		{
			total += (1.0f - Math.abs(ssng.looped_noise(x * freq, z * freq, ws))) * amplitude;
			freq*=2.0f;
			maxAmplitude += amplitude;
			amplitude *= persistence;
		}
		return total / maxAmplitude;
	}
	
	public int getHeightAt(int x, int z)
	{
		float finalHeight = 0.0f;
		//Mountains
		finalHeight += (ridgedNoise(x, z, 5, 1.0f, 0.5f) * 192);
		
		return (int) finalHeight;
	}
	
	public int getDataAt(int x, int y)
	{
		return 1;
	}
}
