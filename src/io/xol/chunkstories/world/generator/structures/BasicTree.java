package io.xol.chunkstories.world.generator.structures;

import io.xol.chunkstories.world.CubicChunk;

public class BasicTree extends GenerableStructure
{

	int height;

	public BasicTree(int x, int y, int z, int h)
	{
		super(x, y, z);
		height = h;
	}

	@Override
	public void draw(CubicChunk c)
	{
		// System.out.println("Drawing ");
		/*
		 * if(oriX-c.chunkX*32 < -5) return; if(oriX-c.chunkX*32 > 37) return;
		 * 
		 * if(oriZ-c.chunkZ*32 < -5) return; if(oriZ-c.chunkZ*32 > 37) return;
		 */
		/*for (int i = height - 4; i < height + 1; i++)
		{
			for (int a = -2; a <= 2; a++)
				for (int b = -2; b <= 2; b++)
				{
					if (Math.abs(a) + Math.abs(b) + Math.abs(height - 2 - i) < 6)
						c.setDataAbsolute(oriX + a, oriY + i, oriZ + b, 5);
				}
		}

		for (int i = 0; i < height; i++)
		{
			c.setDataAbsolute(oriX, oriY + i, oriZ, 4);
		}*/
	}

}
