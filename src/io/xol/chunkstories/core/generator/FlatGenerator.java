package io.xol.chunkstories.core.generator;

import java.util.Random;

import io.xol.chunkstories.api.world.WorldGenerator;
import io.xol.chunkstories.world.World;
import io.xol.chunkstories.world.chunk.CubicChunk;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class FlatGenerator extends WorldGenerator
{
	Random rnd = new Random();
	
	@Override
	public void initialize(World w)
	{
		super.initialize(w);
		ws = world.getSizeInChunks() * 32;
	}
	
	int ws;
	
	@Override
	public CubicChunk generateChunk(int cx, int cy, int cz)
	{
		rnd.setSeed(cx * 32 + cz + 48716148);
		
		CubicChunk c = new CubicChunk(world, cx, cy, cz);
		int type = 12;
		for(int x = 0; x < 32; x++)
			for(int z = 0; z < 32; z++)
			{
				//int v = getHeightAt(cx * 32 + x, cz * 32 + z);
				int v = 30;
				//int v = 250;
				int y = cy * 32;
				while(y < cy * 32 + 32 && y <= v)
				{
					c.setDataAtWithoutUpdates(x, y, z, type);
					y++;
				}
			}
		return c;
	}
	
	
	
	private int getHeightAtInternal(int x, int z)
	{
		return 30;
	}
	
	@Override
	public int getHeightAt(int x, int z)
	{
		int finalHeight = getHeightAtInternal(x, z);
		return finalHeight;
	}
	
	@Override
	public int getDataAt(int x, int z)
	{
		return 12;
	}
}
