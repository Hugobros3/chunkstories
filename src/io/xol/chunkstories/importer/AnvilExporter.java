package io.xol.chunkstories.importer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import io.xol.chunkstories.anvil.MinecraftChunk;
import io.xol.chunkstories.anvil.MinecraftRegion;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelLogic;
import io.xol.chunkstories.content.GameData;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.chunkstories.tools.WorldTool;
import io.xol.chunkstories.voxel.VoxelTypes;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.WorldInfo.WorldSize;
import io.xol.engine.misc.FoldersUtils;
import io.xol.chunkstories.world.WorldInfo;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class AnvilExporter
{

	//This program loads a mcanvil game file and makes a chunk stories world file with them.

	static boolean verbose = false;
	static MinecraftRegion region;

	public static void main(String[] arguments)
	{
		//Start logs
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("YYYY.MM.dd HH.mm.ss");
		String time = sdf.format(cal.getTime());
		ChunkStoriesLogger.init(new ChunkStoriesLogger(ChunkStoriesLogger.LogLevel.ALL, ChunkStoriesLogger.LogLevel.ALL, new File("./logs/" + time + ".log")));

		GameData.reload();

		if (arguments.length < 5)
		{
			System.out.println("Usage : anvil-export anvilWorldDir csWorldDir <size> <x-start> <z-start> [void-fill] [-vr]");
			System.out.println("anvilWorldDir is the directory containing the Minecraft level ( the one with level.dat inside )");
			System.out.println("csWorldDir is the export destination.");
			System.out.println("Target size for chunk stories world, avaible sizes : " + WorldSize.getAllSizes());
			System.out.println("<x-start> and <z-start> are the two coordinates (in mc world) from where we will take the data, " + "going up in the coordinates to fill the world size.\n Exemple : anvil-export mc cs TINY -512 -512 will take the"
					+ "minecraft portion between X:-512 and Z:-512 to fill a 1024x1024 cs level.");
			System.out.println("void-fill designates how you want the void chunks ( : not generated in minecraft) to be filled. default : air");
			System.out.println("-v : verbose mode");
			System.out.println("-r : delete and rewrite destination if already present");
			return;
		}
		else
		{

			long timestampStart = System.currentTimeMillis();

			String mcWorldName = arguments[0];
			File mcWorldDir = new File(mcWorldName);
			if (!mcWorldDir.exists() || !mcWorldDir.isDirectory())
			{
				System.out.println(mcWorldDir + " is not a valid directory.");
				return;
			}

			String csWorldName = arguments[1];
			File csWorldDir = new File("worlds/" + csWorldName + "/");

			WorldSize size = WorldSize.getWorldSize(arguments[2]);
			if (size == null)
			{
				System.out.println("Invalid world size. Valid world sizes : " + WorldSize.getAllSizes());
				return;
			}

			int minecraftOffsetX = Integer.parseInt(arguments[3]);
			int minecraftOffsetZ = Integer.parseInt(arguments[4]);

			//The rest of the parameters now :
			verbose = false;
			boolean deleteAndRewrite = false;
			for (int i = 5; i < arguments.length; i++)
			{
				if (arguments[i].startsWith("-"))
				{
					if (arguments[i].contains("v"))
						verbose = true;
					if (arguments[i].contains("r"))
						deleteAndRewrite = true;
				}
			}

			//Build a cache
			long ict = System.nanoTime();
			verbose("Creating ids conversion cache");
			int[] cachedIdsMatrix = new int[4096 * 16];
			for (int i = 0; i < 4096; i++)
				for (int m = 0; m < 16; m++)
					cachedIdsMatrix[i * 16 + m] = IDsConverter.getChunkStoriesIdFromMinecraft(i, m);
			verbose("Done, took " + (System.nanoTime() - ict) / 1000 + " µs");

			//Cache export
			saveCSV(cachedIdsMatrix);

			// Okay, let's roll motherfuckas
			if (deleteAndRewrite)
			{
				if (csWorldDir.exists())
				{
					FoldersUtils.deleteFolder(csWorldDir);
					verbose(csWorldDir + " already existed, deleting !");
				}
			}
			verbose("Initializing Chunk Stories World");

			WorldInfo info = new WorldInfo("name: Converted_" + mcWorldName + "\n" + "seed: null\n" + "worldgen: blank\n" + "size: " + size.name(), csWorldName);
			info.save(new File(csWorldDir + "/info.txt"));
			WorldImplementation exported = new WorldTool(csWorldDir);

			int mcRegionStartX = c2r(minecraftOffsetX);
			int mcRegionStartZ = c2r(minecraftOffsetZ);

			int mcRegionEndX = c2r(minecraftOffsetX + size.sizeInChunks * 32);
			int mcRegionEndZ = c2r(minecraftOffsetZ + size.sizeInChunks * 32);

			int minecraftChunksImported = 0;
			int minecraftChunksToImport = ((size.sizeInChunks * 32) * (size.sizeInChunks * 32)) / (16 * 16);

			double completion = 0.0;
			long lastPercentageShow = System.currentTimeMillis();

			try
			{
				for (int minecraftRegionX = mcRegionStartX; minecraftRegionX < mcRegionEndX; minecraftRegionX++)
				{
					for (int minecraftRegionZ = mcRegionStartZ; minecraftRegionZ < mcRegionEndZ; minecraftRegionZ++)
					{
						String mcrPath = mcWorldName + "/region/r." + minecraftRegionX + "." + minecraftRegionZ + ".mca";

						File regionFile = new File(mcrPath);
						if (regionFile.exists())
						{
							//verbose("Loading mc region file "+mcrPath);
							region = new MinecraftRegion(regionFile);
						}
						else
						{
							region = null;
							verbose("mc region file " + mcrPath + " doesn't exist !");
						}

						//Iterate over each chunk within the region
						for (int minecraftCurrentChunkXinsideRegion = 0; minecraftCurrentChunkXinsideRegion < 32; minecraftCurrentChunkXinsideRegion++)
						{
							for (int minecraftCuurrentChunkZinsideRegion = 0; minecraftCuurrentChunkZinsideRegion < 32; minecraftCuurrentChunkZinsideRegion++)
							{
								//Map minecraft chunk-space to chunk storie's
								int chunkStoriesCurrentChunkX = (minecraftCurrentChunkXinsideRegion + minecraftRegionX * 32) * 16 - minecraftOffsetX;
								int chunkStoriesCurrentChunkZ = (minecraftCuurrentChunkZinsideRegion + minecraftRegionZ * 32) * 16 - minecraftOffsetZ;

								//Is it within our borders ?
								if (chunkStoriesCurrentChunkX >= 0 && chunkStoriesCurrentChunkX < size.sizeInChunks * 32 && chunkStoriesCurrentChunkZ >= 0 && chunkStoriesCurrentChunkZ < size.sizeInChunks * 32)
								{
									//Load the chunk
									MinecraftChunk chunk = null;
									try
									{
										if (region != null)
											chunk = region.getChunk(minecraftCurrentChunkXinsideRegion, minecraftCuurrentChunkZinsideRegion);
										if (chunk != null)
										{
											for (int x = 0; x < 16; x++)
												for (int z = 0; z < 16; z++)
													for (int y = 0; y < 256; y++)
													{
														//Translate each block
														int mcId = chunk.getBlockID(x, y, z) & 0xFFF;
														int meta = chunk.getBlockMeta(x, y, z) & 0xF;
														//Ignore air blocks
														if (mcId != 0)
														{
															int dataToSet = cachedIdsMatrix[mcId * 16 + meta];//IDsConverter.getChunkStoriesIdFromMinecraft(mcId, meta);
															if (dataToSet == -2)
																dataToSet = IDsConverter.getChunkStoriesIdFromMinecraftComplex(mcId, meta, region, minecraftCurrentChunkXinsideRegion, minecraftCuurrentChunkZinsideRegion, x, y, z);

															if (dataToSet != -1)
															{
																Voxel voxel = VoxelTypes.get(dataToSet);

																//Optionally runs whatever the voxel requires to run when placed
																if (voxel instanceof VoxelLogic)
																	dataToSet = ((VoxelLogic) voxel).onPlace(exported, chunkStoriesCurrentChunkX + x, y, chunkStoriesCurrentChunkZ + z, dataToSet, null);

																if (dataToSet != -1)
																	exported.setVoxelDataWithoutUpdates(chunkStoriesCurrentChunkX + x, y, chunkStoriesCurrentChunkZ + z, dataToSet);
															}
														}
													}
											//Converts external data such as signs
											chunk.postProcess(exported, chunkStoriesCurrentChunkX, 0, chunkStoriesCurrentChunkZ);
										}
									}
									catch (Exception e)
									{
										verbose("Issue with chunk " + minecraftCurrentChunkXinsideRegion + " " + minecraftCuurrentChunkZinsideRegion + " of region " + minecraftRegionX + " " + minecraftRegionZ + ".");
										e.printStackTrace();
									}

									minecraftChunksImported++;
									if (Math.floor(((double) minecraftChunksImported / (double) minecraftChunksToImport) * 100) > completion)
									{
										completion = Math.floor(((double) minecraftChunksImported / (double) minecraftChunksToImport) * 100);

										if (completion >= 100.0 || (System.currentTimeMillis() - lastPercentageShow > 5000))
										{
											verbose(completion + "% ... ");
											lastPercentageShow = System.currentTimeMillis();
										}
									}

								}
								if (exported.getRegionsHolder().countChunksWithData() > 256)
								{
									//Save world
									verbose("More than 256 chunks in memory, saving and unloading before continuing");
									exported.saveEverything();
									exported.unloadEverything();
								}
							}
						}
						//Close region
						if (region != null)
							region.close();
						System.gc();
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

			exported.saveEverything();
			exported.unloadEverything();

			exported.destroy();

			say("Done, conversion took " + (System.currentTimeMillis() - timestampStart) / 1000 + "s");
			//Runtime.getRuntime().exit(0);
		}
	}

	private static void saveCSV(int[] cachedIdsMatrix)
	{
		try
		{
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("converter_ids_cache.csv"), "UTF-8"));
			
			for (int i = 0; i < 256; i++)
			{
				out.write(i+",");
				for(int m = 0; m < 16; m++)
				{
					out.write(""+cachedIdsMatrix[i * 16 + m]);
					if(m < 15)
						out.write(",");
				}
				out.write("\n");
			}
			out.close();
		}
		catch (FileNotFoundException e)
		{
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	static int c2r(int c)
	{
		if (c >= 0)
		{
			return (int) Math.floor(c / 512f);
		}
		c = -c;
		return -(int) Math.floor(c / 512f) - 1;
	}

	static void verbose(String s)
	{
		if (verbose)
		{
			System.out.println(s);
		}
	}

	static void say(String s)
	{
		System.out.println(s);
	}
}
