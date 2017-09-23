package io.xol.chunkstories.core.generator;

import java.util.Random;

import io.xol.chunkstories.api.Content.WorldGenerators.WorldGeneratorType;
import io.xol.chunkstories.api.world.generator.environment.DefaultWorldEnvironment;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.generator.environment.WorldEnvironment;
import io.xol.chunkstories.api.world.generator.WorldGenerator;
import io.xol.chunkstories.api.world.chunk.Chunk;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class FlatGenerator extends WorldGenerator
{
	DefaultWorldEnvironment worldEnv;
	Random rnd = new Random();

	public FlatGenerator(WorldGeneratorType type, World w)
	{
		super(type, w);
		ws = world.getSizeInChunks() * 32;
		worldEnv = new DefaultWorldEnvironment(world);
	}

	int ws;

	@Override
	public Chunk generateChunk(Chunk c)
	{
		int cx = c.getChunkX();
		int cy = c.getChunkY();
		int cz = c.getChunkZ();
		
		rnd.setSeed(cx * 32 + cz + 48716148);

		//CubicChunk c = new CubicChunk(region, cx, cy, cz);
		for (int x = 0; x < 32; x++)
			for (int z = 0; z < 32; z++)
			{
				int type = 27;
				//int v = getHeightAt(cx * 32 + x, cz * 32 + z);
				int v = 21;
				if ((cx * 32 + x) % 256 == 0 || (cz * 32 + z) % 256 == 0)
				{
					v = 30;
				}
				else
					type = 2;
				//int v = 250;
				int y = cy * 32;
				while (y < cy * 32 + 32 && y <= v)
				{
					if (y == 29)
						type = 23;
					if (y == 30)
						type = 25;
					c.setVoxelDataWithoutUpdates(x, y, z, type);
					y++;
				}
			}
		return c;
	}

	@Override
	public int getHeightAt(int x, int z)
	{
		int v = 21;
		if ((x) % 256 == 0 || (z) % 256 == 0)
		{
			v = 30 - 1;
		}
		return v;

		//int finalHeight = getHeightAtInternal(x, z);
		//return finalHeight;
	}

	@Override
	public int getTopDataAt(int x, int z)
	{
		int type = 27;
		if ((x) % 256 == 0 || (z) % 256 == 0)
		{
			type = 23;
		}

		return type;
	}

	@Override
	public WorldEnvironment getEnvironment() {
		return worldEnv;
	}
}
