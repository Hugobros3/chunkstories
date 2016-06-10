package io.xol.chunkstories.tools;

import java.io.File;

import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.world.World;
import io.xol.chunkstories.world.WorldInfo.WorldSize;
import io.xol.chunkstories.world.chunk.CubicChunk;
import io.xol.chunkstories.world.generator.core.BlankWorldGenerator;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class SummaryRegenerator
{

	public static void main(String[] arguments)
	{
		if (arguments.length < 2)
		{
			System.out.println("Usage : summary-regen worldname size");
			return;
		}
		else
		{
			long timestampStart = System.currentTimeMillis();

			String csWorldName = arguments[0];
			File csWorldDir = new File("worlds/" + csWorldName + "/");

			WorldSize size = WorldSize.getWorldSize(arguments[1]);
			if (size == null)
			{
				System.out.println("Invalid world size. Valid world sizes : " + WorldSize.getAllSizes());
				return;
			}
			if (!csWorldDir.exists())
			{
				System.out.println("This world doesn't exist.");
				return;
			}

			World world = new WorldTool(csWorldName, "", new BlankWorldGenerator(), size);
			for (int i = 0; i < world.getSizeInChunks() / 8; i++)
			{
				for (int j = 0; j < world.getSizeInChunks() / 8; j++)
				{
					for (int x = 0; x < 256; x++)
					{
						for (int z = 0; z < 256; z++)
						{
							boolean hit = false;
							int y = world.getMaxHeight();
							int rX = i * 256 + x;
							int rZ = j * 256 + z;
							while (!hit && y > 0)
							{
								int id = VoxelFormat.id(world.getDataAt(rX, y, rZ));
								CubicChunk chunk = world.getChunk(rX / 32, y / 32, rZ / 32, true);
								if (chunk != null && chunk.dataPointer == -1)
								{
									y -= 31;
								}
								else
								{
									if (id != 0)
									{
										hit = true;
										world.getRegionSummaries().set(rX, rZ, y, id);
									}
								}
								y--;
							}
							System.out.println(world.chunksData.free());
							world.saveEverything();
							world.getChunksHolder().clearAll();
							world.unloadEverything();
						}
					}
				}
			}
			world.saveEverything();

			System.out.println("Done, took " + (System.currentTimeMillis() - timestampStart) / 1000 + "s");
		}
	}
}
