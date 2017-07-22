package io.xol.chunkstories.converter;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import io.xol.chunkstories.anvil.MinecraftChunk;
import io.xol.chunkstories.anvil.MinecraftRegion;
import io.xol.chunkstories.anvil.MinecraftWorld;
import io.xol.chunkstories.anvil.nbt.NBTInt;
import io.xol.chunkstories.api.Content;
import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.plugin.PluginManager;
import io.xol.chunkstories.api.util.ChunkStoriesLogger;
import io.xol.chunkstories.api.util.concurrency.Fence;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelLogic;
import io.xol.chunkstories.api.world.WorldInfo;
import io.xol.chunkstories.api.world.WorldInfo.WorldSize;
import io.xol.chunkstories.api.world.chunk.ChunkHolder;
import io.xol.chunkstories.api.world.chunk.WorldUser;
import io.xol.chunkstories.api.world.heightmap.RegionSummary;
import io.xol.chunkstories.content.GameContentStore;
import io.xol.chunkstories.converter.ConverterMapping.Mapper;
import io.xol.chunkstories.converter.ConverterMapping.NonTrivialMapper;
import io.xol.chunkstories.tools.ChunkStoriesLoggerImplementation;
import io.xol.chunkstories.tools.WorldTool;
import io.xol.chunkstories.voxel.VoxelsStore;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.WorldInfoFile;
import io.xol.chunkstories.world.WorldInfoImplementation;
import io.xol.chunkstories.world.summary.RegionSummaryImplementation;
import io.xol.engine.concurrency.CompoundFence;
import io.xol.engine.misc.FoldersUtils;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class OfflineWorldConverter implements GameContext, WorldUser
{
	public static void main(String arguments[]) throws IOException
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
		
		int threadCount = -1;
		
		for (int i = 5; i < arguments.length; i++)
		{
			if (arguments[i].startsWith("-"))
			{
				if (arguments[i].contains("v"))
					verboseMode = true;
				if (arguments[i].contains("r"))
					deleteAndRewrite = true;
			}
			if(arguments[i].startsWith("-mt")) {
				if(arguments[i].startsWith("-mt="))
				{
					String coreCounts = arguments[i].substring(4);
					threadCount = Integer.parseInt(coreCounts);
				}
				else
					threadCount = Runtime.getRuntime().availableProcessors();
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
			FoldersUtils.deleteFolder(csWorldDir);
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
		
		OfflineWorldConverter converter;
		
		System.out.println("threadCount: "+threadCount);
		
		if(threadCount <= 1)
			converter = new OfflineWorldConverter(verboseMode, mcWorldDir, csWorldDir, mcWorldName, csWorldName, size, minecraftOffsetX, minecraftOffsetZ);
		else
			converter = new MultithreadedOfflineWorldConverter(verboseMode, mcWorldDir, csWorldDir, mcWorldName, csWorldName, size, minecraftOffsetX, minecraftOffsetZ, threadCount);
		
		
		converter.run();
	}

	//TODO Does standard vanilla minecraft even support 512 and more worlds now ? No information to be found on the wiki apparently
	public static final int mcWorldHeight = 256;
	
	protected final boolean verboseMode;
	protected final GameContentStore content;
	protected ChunkStoriesLoggerImplementation logger;
	
	//TODO make these configurable
	protected final int targetChunksToKeepInRam = 4096;

	protected final MinecraftWorld mcWorld;
	protected final WorldTool csWorld;
	
	protected final int minecraftOffsetX;
	protected final int minecraftOffsetZ;

	protected final String mcWorldName;
	
	protected final ConverterMapping mappers;
	
	public OfflineWorldConverter(boolean verboseMode, File mcFolder, File csFolder, String mcWorldName, String csWorldName, WorldSize size, int minecraftOffsetX, int minecraftOffsetZ) throws IOException
	{
		this.verboseMode = verboseMode;
		this.minecraftOffsetX = minecraftOffsetX;
		this.minecraftOffsetZ = minecraftOffsetZ;
		this.mcWorldName = mcWorldName;

		//Start logs
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("YYYY.MM.dd HH.mm.ss");
		String time = sdf.format(cal.getTime());
		logger = new ChunkStoriesLoggerImplementation(this, ChunkStoriesLoggerImplementation.LogLevel.ALL, ChunkStoriesLoggerImplementation.LogLevel.ALL, new File("./logs/converter_" + time + ".log"));

		content = new GameContentStore(this, null);
		content.reload();


		verbose("Loading converter_mapping.txt");
		File file = new File("converter_mapping.txt");
		mappers = new ConverterMapping(this, file);
		verbose("Done, took " + (System.nanoTime() - System.nanoTime()) / 1000 + " µs");
		
		//Loads the Minecraft World
		mcWorld = new MinecraftWorld(mcFolder);

		//Creates the ChunkStories world data file
		/*csFolder.mkdirs();
		WorldInfoImplementation info = new WorldInfoImplementation("name: Converted_" + mcFolder + "\n" + "seed: null\n" + "worldgen: blank\n" + "size: " + size.name(), csFolder.getName());

		//Save it and creates the ChunkStories world
		info.save(new File(csFolder.getAbsolutePath() + "/info.world"));*/
		
		String worldGenerator = "blank";
		
		//String csWorldName = "converted_" + mcWorldName;
		String internalName = csWorldName.replaceAll("[^\\w\\s]","_");
		
		String description = "Automatic conversion of the Minecraft map '"+mcWorldName+"'";
		
		Random random = new Random();
		
		WorldInfoImplementation worldInfo = new WorldInfoImplementation(internalName, csWorldName, random.nextLong() + "", description, size, worldGenerator );
		WorldInfoFile worldInfoFile = null;
		try {
			worldInfoFile = WorldInfoFile.createNewWorld(csFolder, worldInfo);
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("Brownout lel");
		}
		
		//IO is NOT blocking here, good luck.
		csWorld = new WorldTool(this, worldInfoFile, false);
	}
	
	public void run() {
		long benchmarkingStart = System.currentTimeMillis();
		
		//Step one: copy the entire world data
		stepOneCopyWorldData(mcWorld, csWorld, minecraftOffsetX, minecraftOffsetZ);
		//Step two: make the summary data for chunk stories
		stepTwoCreateSummaryData(csWorld);
		//Step three: redo the lightning of the entire map
		stepThreeSpreadLightning(csWorld);
		//Step four: fluff
		stetFourTidbits(mcWorld, csWorld);
		
		long timeTook = System.currentTimeMillis() - benchmarkingStart;
		double timeTookSeconds = timeTook / 1000.0;
		
		System.out.println("Done converting "+mcWorldName + ", took "+timeTookSeconds + " seconds.");
	}

	protected void stepOneCopyWorldData(MinecraftWorld mcWorld, WorldImplementation csWorld, int minecraftOffsetX, int minecraftOffsetZ)
	{
		verbose("Entering step one: converting raw block data");

		long ict = System.nanoTime();
		//verbose("Creating ids conversion cache");
		//int[] quickConversion = IDsConverter.generateQuickConversionTable();

		//Prepares the loops
		WorldSize size = csWorld.getWorldInfo().getSize();

		int mcRegionStartX = MinecraftWorld.blockToRegionCoordinates(minecraftOffsetX);
		int mcRegionStartZ = MinecraftWorld.blockToRegionCoordinates(minecraftOffsetZ);

		int mcRegionEndX = MinecraftWorld.blockToRegionCoordinates(minecraftOffsetX + size.sizeInChunks * 32);
		int mcRegionEndZ = MinecraftWorld.blockToRegionCoordinates(minecraftOffsetZ + size.sizeInChunks * 32);

		int minecraftChunksImported = 0;
		long minecraftChunksToImport = ((long)(size.sizeInChunks * 32) * (long)(size.sizeInChunks * 32)) / (16 * 16);

		//System.out.println(size + " " + size.sizeInChunks + " " + minecraftChunksToImport);
		
		double completion = 0.0;
		long lastPercentageShow = System.currentTimeMillis();

		Set<ChunkHolder> registeredCS_Holders = new HashSet<ChunkHolder>();
		//Set<RegionSummary> registeredCS_Summaries = new HashSet<RegionSummary>();
		WorldUser worldUser = this;
		int chunksAquired = 0;

		try
		{
			//We do this minecraft region per minecraft region.
			for (int minecraftRegionX = mcRegionStartX; minecraftRegionX < mcRegionEndX; minecraftRegionX++)
			{
				for (int minecraftRegionZ = mcRegionStartZ; minecraftRegionZ < mcRegionEndZ; minecraftRegionZ++)
				{
					//Load the culprit (There isn't any fancy world management, the getRegion() actually loads the entire region file)
					MinecraftRegion minecraftRegion = mcWorld.getRegion(minecraftRegionX, minecraftRegionZ);

					
					//Iterate over each chunk within the minecraft region
					//TODO Good candidate for task-ifying
					for (int minecraftCurrentChunkXinsideRegion = 0; minecraftCurrentChunkXinsideRegion < 32; minecraftCurrentChunkXinsideRegion++)
					{
						for (int minecraftCuurrentChunkZinsideRegion = 0; minecraftCuurrentChunkZinsideRegion < 32; minecraftCuurrentChunkZinsideRegion++)
						{
							//Map minecraft chunk-space to chunk stories's
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

										//Ignore the summaries for now
										
										/*RegionSummary summary = csWorld.getRegionsSummariesHolder().aquireRegionSummaryWorldCoordinates(this, chunkStoriesCurrentChunkX, chunkStoriesCurrentChunkZ);
										if(summary != null)
											registeredCS_Summaries.add(summary);*/


										CompoundFence loadRelevantData = new CompoundFence();
										
										//Then the chunks
										for (int y = 0; y < mcWorldHeight; y += 32)
										{
											ChunkHolder holder = csWorld.aquireChunkHolderWorldCoordinates(worldUser, chunkStoriesCurrentChunkX, y, chunkStoriesCurrentChunkZ);
											if (holder != null) {
												registeredCS_Holders.add(holder);
												loadRelevantData.add(holder.waitForLoading());
												chunksAquired++;
											}
										}
										
										//Wait for them to actually load
										loadRelevantData.traverse();

										for (int x = 0; x < 16; x++)
											for (int z = 0; z < 16; z++)
												for (int y = 0; y < mcWorldHeight; y++)
												{
													//Translate each block
													int mcId = minecraftChunk.getBlockID(x, y, z) & 0xFFF;
													byte meta = (byte) (minecraftChunk.getBlockMeta(x, y, z) & 0xF);
													
													//Ignore air blocks
													if (mcId != 0)
													{
														/*int dataToSet = quickConversion[mcId * 16 + meta];//IDsConverter.getChunkStoriesIdFromMinecraft(mcId, meta);
														if (dataToSet == -2)
															dataToSet = IDsConverter.getChunkStoriesIdFromMinecraftComplex(mcId, meta, minecraftRegion, minecraftCurrentChunkXinsideRegion, minecraftCuurrentChunkZinsideRegion, x, y, z);

														if (dataToSet != -1)
														{
															Voxel voxel = VoxelsStore.get().getVoxelById(dataToSet);

															//Optionally runs whatever the voxel requires to run when placed (kof kof .. doors )
															if (voxel instanceof VoxelLogic)
																dataToSet = ((VoxelLogic) voxel).onPlace(csWorld, chunkStoriesCurrentChunkX + x, y, chunkStoriesCurrentChunkZ + z, dataToSet, null);

															//Don't bother for nothing
															if (dataToSet != -1)
																csWorld.setVoxelDataWithoutUpdates(chunkStoriesCurrentChunkX + x, y, chunkStoriesCurrentChunkZ + z, dataToSet);
														}*/
														
														Mapper mapper = this.mappers.getMapper(mcId, meta);
														if(mapper == null)
															continue;
														
														if(mapper instanceof NonTrivialMapper) {
															((NonTrivialMapper)mapper).output(csWorld, chunkStoriesCurrentChunkX + x, y, chunkStoriesCurrentChunkZ + z, mcId, meta, minecraftRegion, minecraftCurrentChunkXinsideRegion, minecraftCuurrentChunkZinsideRegion, x, y, z);
														} else {
															
															//Directly set trivial blocks
															int trivial = mapper.output(mcId, meta);
															if(trivial != 0x0) {
																csWorld.setVoxelDataWithoutUpdates(chunkStoriesCurrentChunkX + x, y, chunkStoriesCurrentChunkZ + z, trivial);
															}
														}
													}
												}

										//Converts external data such as signs
										SpecialBlocksHandler.processAdditionalStuff(minecraftChunk, csWorld, chunkStoriesCurrentChunkX, 0, chunkStoriesCurrentChunkZ);
									}
								}
								catch (Exception e)
								{
									verbose("Issue with chunk " + minecraftCurrentChunkXinsideRegion + " " + minecraftCuurrentChunkZinsideRegion + " of region " + minecraftRegionX + " " + minecraftRegionZ + ".");
									e.printStackTrace();
								}

								//Display progress
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

							if (chunksAquired > targetChunksToKeepInRam)
							{
								//Save world
								verbose("More than "+targetChunksToKeepInRam+" chunks already in memory, saving and unloading before continuing");
								
								//Redudant, see below
								//csWorld.saveEverything();

								for (ChunkHolder holder : registeredCS_Holders) {
									holder.unregisterUser(worldUser);
									chunksAquired--;
								}

								//registeredCS_Summaries.clear();
								registeredCS_Holders.clear();

								//Automatically saves what we don't need anymore because this is a master world
								csWorld.unloadUselessData().traverse();
								verbose("Done.");
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

		csWorld.unloadUselessData();
		//csWorld.saveEverything().traverse();
		//csWorld.unloadEverything();
	}

	protected void stepTwoCreateSummaryData(WorldTool csWorld)
	{
		verbose("Entering step two: making summary data");

		WorldSize size = csWorld.getWorldInfo().getSize();

		int maxHeightPossible = 256;

		int done = 0;
		int todo = (size.sizeInChunks / 8) * (size.sizeInChunks / 8);

		double completion = 0.0;
		long lastPercentageShow = System.currentTimeMillis();

		WorldUser worldUser = this;
		Set<ChunkHolder> registeredCS_Holders = new HashSet<ChunkHolder>();
		
		for (int regionX = 0; regionX < size.sizeInChunks / 8; regionX++)
			for (int regionZ = 0; regionZ < size.sizeInChunks / 8; regionZ++)
			{
				
				//We wait on a bunch of stuff to load everytime
				CompoundFence loadRelevantData = new CompoundFence();
				
				RegionSummaryImplementation summary = csWorld.getRegionsSummariesHolder().aquireRegionSummary(worldUser, regionX, regionZ);
				loadRelevantData.add(summary.waitForLoading());

				//Aquires the chunks we want to make the summaries of.
				for (int innerCX = 0; innerCX < 8; innerCX++)
					for (int innerCZ = 0; innerCZ < 8; innerCZ++)
						for (int chunkY = 0; chunkY < maxHeightPossible / 32; chunkY++)
						{
							ChunkHolder holder = csWorld.aquireChunkHolder(worldUser, regionX * 8 + innerCX, chunkY, regionZ * 8 + innerCZ);
							if (holder != null) {
								registeredCS_Holders.add(holder);
								loadRelevantData.add(holder.waitForLoading());
							}
						}
				
				//Wait until all of that crap loads
				loadRelevantData.traverse();

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
				
				Fence waitForSummarySave = summary.saveSummary();
				verbose("Waiting for summary saving...");
				waitForSummarySave.traverse();
				verbose("Done.");

				//Display progress...
				if (Math.floor(((double) done / (double) todo) * 100) > completion)
				{
					completion = Math.floor(((double) done / (double) todo) * 100);

					if (completion >= 100.0 || (System.currentTimeMillis() - lastPercentageShow > 5000))
					{
						verbose(completion + "% ... using " + Runtime.getRuntime().freeMemory() / 1024 / 1024 + "/" + Runtime.getRuntime().maxMemory() / 1024 / 1024 + "Mb ");
						lastPercentageShow = System.currentTimeMillis();
					}
				}

				//We don't need those chunks anymore
				for (ChunkHolder holder : registeredCS_Holders)
					holder.unregisterUser(worldUser);

				//Neither do we do the summary
				summary.unregisterUser(worldUser);
				
				registeredCS_Holders.clear();

				verbose("Saving unused chunk data...");
				csWorld.unloadUselessData().traverse();
				verbose("Done.");
			}
	}

	protected void stepThreeSpreadLightning(WorldImplementation csWorld)
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

		int chunksAquired = 0;
		WorldUser worldUser = this;
		
		for (int chunkX = 0; chunkX < size.sizeInChunks; chunkX++)
			for (int chunkZ = 0; chunkZ < size.sizeInChunks; chunkZ++)
			{
				CompoundFence loadRelevantData = new CompoundFence();
				
				//RegionSummary sum = csWorld.getRegionsSummariesHolder().getRegionSummaryChunkCoordinates(chunkX, chunkZ);
				//if (sum == null)
				//{
					//System.out.println("Loading missing summary");
				
				RegionSummary sum = csWorld.getRegionsSummariesHolder().aquireRegionSummaryChunkCoordinates(worldUser, chunkX, chunkZ);
				registeredCS_Summaries.add(sum);
				loadRelevantData.add(sum.waitForLoading());
				
				//}

				//Loads 3x3 arround relevant chunks
				for (int i = -1; i < 2; i++)
					for (int j = -1; j < 2; j++)
						for (int chunkY = 0; chunkY <= maxHeightPossible / 32; chunkY++)
						{
							ChunkHolder holder = csWorld.aquireChunkHolder(worldUser, chunkX + i, chunkY, chunkZ + j);
							if (holder != null) {
								registeredCS_Holders.add(holder);
								loadRelevantData.add(holder.waitForLoading());
								chunksAquired++;
							}
						}
				
				//Wait for everything to actually load
				loadRelevantData.traverse();

				//Spreads lightning, from top to botton
				for (int chunkY = maxHeightPossible / 32; chunkY >= 0; chunkY--)
				{
					csWorld.getChunk(chunkX, chunkY, chunkZ).computeVoxelLightning(true);
				}

				//Show progress
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
			
				if (chunksAquired > targetChunksToKeepInRam)
				{
					//Save world
					verbose("More than "+targetChunksToKeepInRam+" chunks already in memory, saving and unloading before continuing");
					
					//csWorld.saveEverything();
					//for(Region region : registeredCS_Regions)
					//	region.unregisterUser(user);

					for (ChunkHolder holder : registeredCS_Holders) {
						holder.unregisterUser(worldUser);
						chunksAquired--;
					}

					for (RegionSummary summary : registeredCS_Summaries)
						summary.unregisterUser(worldUser);

					registeredCS_Summaries.clear();
					registeredCS_Holders.clear();

					csWorld.unloadUselessData().traverse();
					verbose("Done.");
				}
			}

		//Terminate
		csWorld.saveEverything();
		for (ChunkHolder holder : registeredCS_Holders)
			holder.unregisterUser(worldUser);

		csWorld.unloadUselessData().traverse();
	}

	protected void stetFourTidbits(MinecraftWorld mcWorld, WorldImplementation csWorld)
	{
		verbose("Entering step four: tidbits");
		
		int spawnX = ((NBTInt) mcWorld.getLevelDotDat().getRoot().getTag("Data.SpawnX")).getData();
		int spawnY = ((NBTInt) mcWorld.getLevelDotDat().getRoot().getTag("Data.SpawnY")).getData();
		int spawnZ = ((NBTInt) mcWorld.getLevelDotDat().getRoot().getTag("Data.SpawnZ")).getData();
		
		csWorld.setDefaultSpawnLocation(new Location(csWorld, spawnX, spawnY, spawnZ));
		csWorld.saveEverything().traverse();
		
		csWorld.destroy();
	}
	
	protected void verbose(String s)
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

	@Override
	public ChunkStoriesLogger logger() {
		return logger;
	}
}
