package io.xol.chunkstories.world.generator;

import java.util.Random;

import io.xol.chunkstories.api.world.WorldGenerator;
import io.xol.chunkstories.world.CubicChunk;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class HorizonGenerator extends WorldGenerator
{
	Random rnd = new Random();
	
	@Override
	public CubicChunk generateChunk(int cx, int cy, int cz)
	{
		rnd.setSeed(cx * 32 + cz + 48716148);
		
		CubicChunk c = new CubicChunk(world, cx, cy, cz);
		for(int x = 0; x < 32; x++)
			for(int z = 0; z < 32; z++)
			{
				int v = 50;
				int y = cy * 32;
				while(y < cy * 32 + 32 && y < v)
				{
					c.setDataAt(x, y, z, 1);
					y++;
				}
			}
		// c.dataPointer = world.chunksData.malloc();
		/*
		 * for(int a = 0; a < 32; a++) for(int b = 0; b < 32; b++) { for(int i =
		 * 0; i < 32; i++) { if(Math.random() > 0.999) c.setDataAt(a, i, b, 1);
		 * } }
		 */
		// System.out.println("Loading chunk "+cx+":"+cy+":"+cz+" set dp"+c.dataPointer);
		return c;
	}
	
	public int getHeightAt(int x, int z)
	{
		return 50;
	}
	
	public int getDataAt(int x, int y)
	{
		return 1;
	}
}
