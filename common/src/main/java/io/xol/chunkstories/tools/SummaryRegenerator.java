package io.xol.chunkstories.tools;

import java.io.File;
import java.io.IOException;

import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.world.WorldInfo;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.WorldInfoFile;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

//TODO remake.
public class SummaryRegenerator
{

	public static void main(String[] arguments) throws IOException
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

			WorldInfo.WorldSize size = WorldInfo.WorldSize.getWorldSize(arguments[1]);
			if (size == null)
			{
				System.out.println("Invalid world size. Valid world sizes : " + WorldInfo.WorldSize.getAllSizes());
				return;
			}
			if (!csWorldDir.exists())
			{
				System.out.println("This world doesn't exist.");
				return;
			}

			//TODO let implement GameContext
			WorldImplementation world = new WorldTool(null, new WorldInfoFile(new File(csWorldDir + "/info.world")));//, "", new BlankWorldGenerator(), size);
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
								int id = VoxelFormat.id(world.peekSimple(rX, y, rZ));
								Chunk chunk = world.getChunk(rX / 32, y / 32, rZ / 32);
								if (chunk != null && chunk.isAirChunk())
								{
									y -= 31;
								}
								else
								{
									if (id != 0)
									{
										hit = true;
										world.getRegionsSummariesHolder().setHeightAndId(rX, rZ, y, id);
									}
								}
								y--;
							}
							//System.out.println(world.chunksData.free());
							world.saveEverything();
							//world.getRegionsHolder().clearAll();
							//world.unloadEverything();
						}
					}
				}
			}
			world.saveEverything();

			System.out.println("Done, took " + (System.currentTimeMillis() - timestampStart) / 1000 + "s");
		}
	}
}
