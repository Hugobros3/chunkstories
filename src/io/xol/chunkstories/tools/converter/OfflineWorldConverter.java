package io.xol.chunkstories.tools.converter;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import io.xol.chunkstories.anvil.MinecraftChunk;
import io.xol.chunkstories.anvil.MinecraftRegion;
import io.xol.chunkstories.anvil.MinecraftWorld;
import io.xol.chunkstories.anvil.nbt.NBTInt;
import io.xol.chunkstories.api.Content;
import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.plugin.PluginManager;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelLogic;
import io.xol.chunkstories.api.world.WorldInfo;
import io.xol.chunkstories.api.world.WorldInfo.WorldSize;
import io.xol.chunkstories.api.world.chunk.ChunkHolder;
import io.xol.chunkstories.api.world.chunk.WorldUser;
import io.xol.chunkstories.api.world.heightmap.RegionSummary;
import io.xol.chunkstories.content.GameContentStore;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.chunkstories.tools.WorldTool;
import io.xol.chunkstories.voxel.VoxelsStore;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.WorldInfoImplementation;
import io.xol.chunkstories.world.summary.RegionSummaryImplementation;
import io.xol.engine.misc.FoldersUtils;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class OfflineWorldConverter implements GameContext, WorldUser
{
	public static void main(String arguments[])
	{
		//Parse arguments first
		if (arguments.length < 5)
		{
			System.out.println("Usage : anvil-export anvilWorldDir csWorldDir <size> <x-start> <z-start> [void-fill] [-vr]");
			System.out.println("anvilWorldDir is the directory containing the Minecraft level ( the one with level.dat inside )");
			System.out.println("csWorldDir is the export destination.");
			System.out.println("Target size for chunk stories world, avaible sizes : " + WorldInfo.WorldSize.getAllSizes());
			System.out.println("<x-start> and <z-start> are the two coordinates (in mc world) from where we will take the data, " + "going up in the coordinates to fill the world size.\n Exemple : anvil-export mc cs TINY -512 -512 will take the"
					+ "minecraft portion between X:-512 and Z:-512 to fill a 1024x1024 cs level.");
			System.out.println("void-fill designates how you want the void chunks ( : not generated in minecraft) to be filled. default : air");
			System.out.println("-v : verbose mode");
			System.out.println("-r : delete and rewrite destination if already present");
			return;
		}

		//Modifiers
		boolean verboseMode = false;
		boolean deleteAndRewrite = false;
		for (int i = 5; i < arguments.length; i++)
		{
			if (arguments[i].startsWith("-"))
			{
				if (arguments[i].contains("v"))
					verboseMode = true;
				if (arguments[i].contains("r"))
					deleteAndRewrite = true;
			}
		}

		String mcWorldName = arguments[0];
		File mcWorldDir = new File(mcWorldName);
		if (!mcWorldDir.exists() || !mcWorldDir.isDirectory())
		{
			System.out.println(mcWorldDir + " is not a valid directory.");
			return;
		}

		String csWorldName = arguments[1];
		File csWorldDir = new File("worlds/" + csWorldName + "/");
		if (csWorldDir.exists() && !deleteAndRewrite)
		{
			System.out.println("Destination world " + csWorldName + " already exists in worlds/" + csWorldName + ", aborting.");
			System.out.println("To force deleting and rewriting of this world, at your risk of loosing data, plese use the -r flag.");
			return;
		}
		else if (csWorldDir.exists() && deleteAndRewrite)
		{
			System.out.println("Deleting older world " + csWorldDir);
			//FoldersUtils.deleteFolder(csWorldDir);
		}

		WorldInfo.WorldSize size = WorldInfo.WorldSize.getWorldSize(arguments[2]);
		if (size == null)
		{
			System.out.println("Invalid world size. Valid world sizes : " + WorldInfo.WorldSize.getAllSizes());
			return;
		}

		int minecraftOffsetX = Integer.parseInt(arguments[3]);
		int minecraftOffsetZ = Integer.parseInt(arguments[4]);
		if (minecraftOffsetX % 32 != 0 || minecraftOffsetZ % 32 != 0)
		{
			System.out.println("<x-start> and <z-start> offsets need to be multiples of 32.");
			return;
		}

		//Finally start the conversion
		OfflineWorldConverter converter = new OfflineWorldConverter(verboseMode, mcWorldDir, csWorldDir, size, minecraftOffsetX, minecraftOffsetZ);
	}

	private final boolean verboseMode;
	private final GameContentStore content;

	public OfflineWorldConverter(boolean verboseMode, File mcFolder, File csFolder, WorldSize size, int minecraftOffsetX, int minecraftOffsetZ)
	{
		this.verboseMode = verboseMode;

		//Start logs
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("YYYY.MM.dd HH.mm.ss");
		String time = sdf.format(cal.getTime());
		ChunkStoriesLogger.init(new ChunkStoriesLogger(ChunkStoriesLogger.LogLevel.ALL, ChunkStoriesLogger.LogLevel.ALL, new File("./logs/converter_" + time + ".log")));

		content = new GameContentStore(this, null);

		//Loads the Minecraft World
		MinecraftWorld mcWorld = new MinecraftWorld(mcFolder);

		//Creates the ChunkStories world data file
		csFolder.mkdirs();
		WorldInfoImplementation info = new WorldInfoImplementation("name: Converted_" + mcFolder + "\n" + "seed: null\n" + "worldgen: blank\n" + "size: " + size.name(), csFolder.getName());

		//Save it and creates the ChunkStories world
		info.save(new File(csFolder.getAbsolutePath() + "/info.txt"));
		WorldImplementation csWorld = new WorldTool(this, csFolder);

		//Step one: copy the entire world data
		stepOneCopyWorldData(mcWorld, csWorld, minecraftOffsetX, minecraftOffsetZ);
		//Step two: make the summary data for chunk stories
		stepTwoCreateSummaryData(csWorld);
		//Step three: redo the lightning of the entire map
		stepThreeSpreadLightning(csWorld);
		//Step four: fluff
		stetFourTidbits(mcWorld, csWorld);
	}

	private void stepOneCopyWorldData(MinecraftWorld mcWorld, WorldImplementation csWorld, int minecraftOffsetX, int minecraftOffsetZ)
	{
		verbose("Entering step one: making summary data");

		//Create a conversion table
		long ict = System.nanoTime();
		verbose("Creating ids conversion cache");
		int[] cachedIdsMatrix = new int[4096 * 16];
		for (int i = 0; i < 4096; i++)
			for (int m = 0; m < 16; m++)
				cachedIdsMatrix[i * 16 + m] = IDsConverter.getChunkStoriesIdFromMinecraft(i, m);
		verbose("Done, took " + (System.nanoTime() - ict) / 1000 + " µs");

		//Prepares the loops
		WorldSize size = csWorld.getWorldInfo().getSize();

		int mcRegionStartX = MinecraftWorld.blockToRegionCoordinates(minecraftOffsetX);
		int mcRegionStartZ = MinecraftWorld.blockToRegionCoordinates(minecraftOffsetZ);

		int mcRegionEndX = MinecraftWorld.blockToRegionCoordinates(minecraftOffsetX + size.sizeInChunks * 32);
		int mcRegionEndZ = MinecraftWorld.blockToRegionCoordinates(minecraftOffsetZ + size.sizeInChunks * 32);

		int minecraftChunksImported = 0;
		int minecraftChunksToImport = ((size.sizeInChunks * 32) * (size.sizeInChunks * 32)) / (16 * 16);

		double completion = 0.0;
		long lastPercentageShow = System.currentTimeMillis();

		Set<ChunkHolder> registeredCS_Holders = new HashSet<ChunkHolder>();
		//Set<RegionSummary> registeredCS_Summaries = new HashSet<RegionSummary>();

		//TODO Does minecraft even support 512 and more worlds ? No information to be found on the wiki apparently
		int mcWorldHeight = 256;

		try
		{
			for (int minecraftRegionX = mcRegionStartX; minecraftRegionX < mcRegionEndX; minecraftRegionX++)
			{
				for (int minecraftRegionZ = mcRegionStartZ; minecraftRegionZ < mcRegionEndZ; minecraftRegionZ++)
				{
					MinecraftRegion minecraftRegion = mcWorld.getRegion(minecraftRegionX, minecraftRegionZ);

					//Iterate over each chunk within the minecraft region
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
								MinecraftChunk minecraftChunk = null;
								try
								{
									//Tries loading the Minecraft chunk
									if (minecraftRegion != null)
										minecraftChunk = minecraftRegion.getChunk(minecraftCurrentChunkXinsideRegion, minecraftCuurrentChunkZinsideRegion);

									if (minecraftChunk != null)
									{
										//If it succeed, we first require to load the corresponding chunkstories stuff

										//Ignore the summaries
										/*RegionSummary summary = csWorld.getRegionsSummariesHolder().aquireRegionSummaryWorldCoordinates(this, chunkStoriesCurrentChunkX, chunkStoriesCurrentChunkZ);
										if(summary != null)
											registeredCS_Summaries.add(summary);*/

										//Then the chunks
										for (int y = 0; y < mcWorldHeight; y += 32)
										{
											ChunkHolder holder = csWorld.aquireChunkHolderWorldCoordinates(this, chunkStoriesCurrentChunkX, y, chunkStoriesCurrentChunkZ);
											if (holder != null)
												registeredCS_Holders.add(holder);
										}

										for (int x = 0; x < 16; x++)
											for (int z = 0; z < 16; z++)
												for (int y = 0; y < mcWorldHeight; y++)
												{
													//Translate each block
													int mcId = minecraftChunk.getBlockID(x, y, z) & 0xFFF;
													int meta = minecraftChunk.getBlockMeta(x, y, z) & 0xF;
													//Ignore air blocks
													if (mcId != 0)
													{
														int dataToSet = cachedIdsMatrix[mcId * 16 + meta];//IDsConverter.getChunkStoriesIdFromMinecraft(mcId, meta);
														if (dataToSet == -2)
															dataToSet = IDsConverter.getChunkStoriesIdFromMinecraftComplex(mcId, meta, minecraftRegion, minecraftCurrentChunkXinsideRegion, minecraftCuurrentChunkZinsideRegion, x, y, z);

														if (dataToSet != -1)
														{
															Voxel voxel = VoxelsStore.get().getVoxelById(dataToSet);

															//Optionally runs whatever the voxel requires to run when placed
															if (voxel instanceof VoxelLogic)
																dataToSet = ((VoxelLogic) voxel).onPlace(csWorld, chunkStoriesCurrentChunkX + x, y, chunkStoriesCurrentChunkZ + z, dataToSet, null);

															if (dataToSet != -1)
																csWorld.setVoxelDataWithoutUpdates(chunkStoriesCurrentChunkX + x, y, chunkStoriesCurrentChunkZ + z, dataToSet);
														}
													}
												}

										//Converts external data such as signs
										minecraftChunk.postProcess(csWorld, chunkStoriesCurrentChunkX, 0, chunkStoriesCurrentChunkZ);
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
										verbose(completion + "% ... (" + csWorld.getRegionsHolder().countChunks() + " chunks loaded ) using " + Runtime.getRuntime().freeMemory() / 1024 / 1024 + "/" + Runtime.getRuntime().maxMemory() / 1024 / 1024
												+ "Mb ");
										lastPercentageShow = System.currentTimeMillis();
									}
								}

							}

							//System.out.println(csWorld.getRegionsHolder().countChunks());
							if (csWorld.getRegionsHolder().countChunks() > 256)
							{
								//Save world
								//verbose("More than 256 chunks already in memory, saving and unloading before continuing");
								csWorld.saveEverything();
								//for(Region region : registeredCS_Regions)
								//	region.unregisterUser(user);

								for (ChunkHolder holder : registeredCS_Holders)
									holder.unregisterUser(this);

								//for(RegionSummary summary : registeredCS_Summaries)
								//	summary.unregisterUser(this);

								//registeredCS_Summaries.clear();
								registeredCS_Holders.clear();

								csWorld.unloadUselessData();
							}
						}
					}
					//Close region
					if (minecraftRegion != null)
						minecraftRegion.close();
					System.gc();
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		csWorld.saveEverything();
		csWorld.unloadEverything();
	}

	private void stepTwoCreateSummaryData(WorldImplementation csWorld)
	{
		verbose("Entering step two: making summary data");

		WorldSize size = csWorld.getWorldInfo().getSize();

		int maxHeightPossible = 256;

		int done = 0;
		int todo = (size.sizeInChunks / 8) * (size.sizeInChunks / 8);

		double completion = 0.0;
		long lastPercentageShow = System.currentTimeMillis();

		Set<ChunkHolder> registeredCS_Holders = new HashSet<ChunkHolder>();
		for (int regionX = 0; regionX < size.sizeInChunks / 8; regionX++)
			for (int regionZ = 0; regionZ < size.sizeInChunks / 8; regionZ++)
			{
				RegionSummaryImplementation summary = csWorld.getRegionsSummariesHolder().aquireRegionSummary(this, regionX, regionZ);
				//System.out.println(regionX+":"+regionZ);

				//Aquires the summaries.
				for (int innerCX = 0; innerCX < 8; innerCX++)
					for (int innerCZ = 0; innerCZ < 8; innerCZ++)
						for (int chunkY = 0; chunkY < maxHeightPossible / 32; chunkY++)
						{
							ChunkHolder holder = csWorld.aquireChunkHolder(this, regionX * 8 + innerCX, chunkY, regionZ * 8 + innerCZ);
							if (holder != null)
								registeredCS_Holders.add(holder);

							//System.out.println((regionX * 8 + innerCX)+":"+ chunkY+":"+ (regionZ * 8 + innerCZ)+" "+registeredCS_Holders.size()+"   "+holder);
						}

				//Descend from top
				for (int i = 0; i < 256; i++)
					for (int j = 0; j < 256; j++)
					{
						for (int h = maxHeightPossible; h > 0; h--)
						{
							int data = csWorld.getVoxelData(regionX * 256 + i, h, regionZ * 256 + j);
							if (data != 0)
							{
								Voxel vox = this.getContent().voxels().getVoxelById(data);
								if (vox.getType().isSolid() || vox.getType().isLiquid())
								{
									summary.setHeightAndId(regionX * 256 + i, h, regionZ * 256 + j, data & 0x0000FFFF);
									break;
								}
							}
						}
					}

				done++;
				summary.saveSummary();

				if (Math.floor(((double) done / (double) todo) * 100) > completion)
				{
					completion = Math.floor(((double) done / (double) todo) * 100);

					if (completion >= 100.0 || (System.currentTimeMillis() - lastPercentageShow > 5000))
					{
						verbose(completion + "% ... using " + Runtime.getRuntime().freeMemory() / 1024 / 1024 + "/" + Runtime.getRuntime().maxMemory() / 1024 / 1024 + "Mb ");
						lastPercentageShow = System.currentTimeMillis();
					}
				}

				for (ChunkHolder holder : registeredCS_Holders)
					holder.unregisterUser(this);

				registeredCS_Holders.clear();

				csWorld.unloadUselessData();
			}
	}

	private void stepThreeSpreadLightning(WorldImplementation csWorld)
	{
		verbose("Entering step three: spreading light");

		WorldSize size = csWorld.getWorldInfo().getSize();

		int maxHeightPossible = 256;

		int done = 0;
		int todo = (size.sizeInChunks) * (size.sizeInChunks);

		double completion = 0.0;
		long lastPercentageShow = System.currentTimeMillis();

		Set<ChunkHolder> registeredCS_Holders = new HashSet<ChunkHolder>();
		Set<RegionSummary> registeredCS_Summaries = new HashSet<RegionSummary>();

		for (int chunkX = 0; chunkX < size.sizeInChunks; chunkX++)
			for (int chunkZ = 0; chunkZ < size.sizeInChunks; chunkZ++)
			{
				RegionSummary sum = csWorld.getRegionsSummariesHolder().getRegionSummaryChunkCoordinates(chunkX, chunkZ);
				if (sum == null)
				{
					//System.out.println("Loading missing summary");
					sum = csWorld.getRegionsSummariesHolder().aquireRegionSummaryChunkCoordinates(this, chunkX, chunkZ);
					registeredCS_Summaries.add(sum);
				}

				//Loads 3x3 arround relevant chunks
				for (int i = -1; i < 2; i++)
					for (int j = -1; j < 2; j++)
						for (int chunkY = 0; chunkY <= maxHeightPossible / 32; chunkY++)
						{
							ChunkHolder holder = csWorld.aquireChunkHolder(this, chunkX + i, chunkY, chunkZ + j);
							if (holder != null)
								registeredCS_Holders.add(holder);
						}

				//Spreads lightning, from top to botton
				for (int chunkY = maxHeightPossible / 32; chunkY >= 0; chunkY--)
				{
					csWorld.getChunk(chunkX, chunkY, chunkZ).bakeVoxelLightning(true);
				}

				done++;

				if (Math.floor(((double) done / (double) todo) * 100) > completion)
				{
					completion = Math.floor(((double) done / (double) todo) * 100);

					if (completion >= 100.0 || (System.currentTimeMillis() - lastPercentageShow > 5000))
					{
						verbose(completion + "% ... using " + Runtime.getRuntime().freeMemory() / 1024 / 1024 + "/" + Runtime.getRuntime().maxMemory() / 1024 / 1024 + "Mb ");
						lastPercentageShow = System.currentTimeMillis();
					}
				}

				if (csWorld.getRegionsHolder().countChunks() > 256)
				{
					//Save world
					//verbose("More than 256 chunks already in memory, saving and unloading before continuing");
					csWorld.saveEverything();
					//for(Region region : registeredCS_Regions)
					//	region.unregisterUser(user);

					for (ChunkHolder holder : registeredCS_Holders)
						holder.unregisterUser(this);

					for (RegionSummary summary : registeredCS_Summaries)
						summary.unregisterUser(this);

					registeredCS_Summaries.clear();
					registeredCS_Holders.clear();

					csWorld.unloadUselessData();
				}
			}

		//Terminate
		csWorld.saveEverything();
		for (ChunkHolder holder : registeredCS_Holders)
			holder.unregisterUser(this);

		csWorld.unloadUselessData();
	}

	private void stetFourTidbits(MinecraftWorld mcWorld, WorldImplementation csWorld)
	{
		verbose("Entering step four: tidbits");
		
		int spawnX = ((NBTInt) mcWorld.getLevelDotDat().getRoot().getTag("Data.SpawnX")).getData();
		int spawnY = ((NBTInt) mcWorld.getLevelDotDat().getRoot().getTag("Data.SpawnY")).getData();
		int spawnZ = ((NBTInt) mcWorld.getLevelDotDat().getRoot().getTag("Data.SpawnZ")).getData();
		
		csWorld.setDefaultSpawnLocation(new Location(csWorld, spawnX, spawnY, spawnZ));
		csWorld.saveEverything();
	}
	
	private void verbose(String s)
	{
		if (verboseMode)
		{
			System.out.println(s);
		}
	}

	@Override
	public Content getContent()
	{
		return content;
	}

	@Override
	public PluginManager getPluginManager()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void print(String message)
	{
		System.out.println("GameContext:" + message);
	}
}
