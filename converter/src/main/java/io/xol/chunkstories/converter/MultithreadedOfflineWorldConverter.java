package io.xol.chunkstories.converter;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.workers.Tasks;
import io.xol.chunkstories.api.world.WorldInfo.WorldSize;
import io.xol.chunkstories.api.world.chunk.ChunkHolder;
import io.xol.chunkstories.api.world.chunk.WorldUser;
import io.xol.chunkstories.api.world.heightmap.RegionSummary;
import io.xol.chunkstories.tools.WorldTool;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.chunk.CubicChunk;
import io.xol.chunkstories.world.chunk.TaskLightChunk;
import io.xol.engine.concurrency.CompoundFence;
import io.xol.enklume.MinecraftChunk;
import io.xol.enklume.MinecraftRegion;
import io.xol.enklume.MinecraftWorld;
import io.xol.enklume.nbt.NBTInt;

public class MultithreadedOfflineWorldConverter extends OfflineWorldConverter {

	private final int threadsCount;
	private final ConverterWorkers workers;
	
	public MultithreadedOfflineWorldConverter(boolean verboseMode, File mcFolder, File csFolder, String mcWorldName, String csWorldName, WorldSize size, int minecraftOffsetX, int minecraftOffsetZ, File coreContentLocation, int threadsCount) throws IOException {
		super(verboseMode, mcFolder, csFolder, mcWorldName, csWorldName, size, minecraftOffsetX, minecraftOffsetZ, coreContentLocation);
		
		this.threadsCount = threadsCount;
		this.workers = new ConverterWorkers(this, this.csWorld, threadsCount);
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
		
		//Destroy the workers or it won't do shit
		this.workers.destroy();
		
		System.out.println("Done converting "+mcWorldName + ", took "+timeTookSeconds + " seconds.");
	}
	

	protected void stepOneCopyWorldData(MinecraftWorld mcWorld, WorldImplementation csWorld, int minecraftOffsetX,
			int minecraftOffsetZ) {

		verbose("Entering step one: converting raw block data");

		long ict = System.nanoTime();
		/*verbose("Creating ids conversion cache");
		int[] quickConversion = IDsConverter.generateQuickConversionTable();
		verbose("Done, took " + (System.nanoTime() - ict) / 1000 + " µs");*/

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

		try
		{
			//We do this minecraft region per minecraft region.
			for (int minecraftRegionX = mcRegionStartX; minecraftRegionX < mcRegionEndX; minecraftRegionX++)
			{
				for (int minecraftRegionZ = mcRegionStartZ; minecraftRegionZ < mcRegionEndZ; minecraftRegionZ++)
				{
					//Load the culprit (There isn't any fancy world management, the getRegion() actually loads the entire region file)
					MinecraftRegion minecraftRegion = mcWorld.getRegion(minecraftRegionX, minecraftRegionZ);

					CompoundFence waitForTheBoys = new CompoundFence();
					
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
							if (chunkStoriesCurrentChunkX >= 0 && chunkStoriesCurrentChunkX < csWorld.getWorldInfo().getSize().sizeInChunks * 32 && chunkStoriesCurrentChunkZ >= 0 && chunkStoriesCurrentChunkZ < csWorld.getWorldInfo().getSize().sizeInChunks * 32)
							{

								if (minecraftRegion != null) {
									MinecraftChunk minecraftChunk = minecraftRegion.getChunk(minecraftCurrentChunkXinsideRegion, minecraftCuurrentChunkZinsideRegion);
								
									TaskConvertMcChunk task = new TaskConvertMcChunk(minecraftRegion, minecraftChunk, chunkStoriesCurrentChunkX, chunkStoriesCurrentChunkZ, minecraftCurrentChunkXinsideRegion, minecraftCuurrentChunkZinsideRegion, minecraftRegionX, minecraftRegionZ, mappers);
									workers.scheduleTask(task);
									waitForTheBoys.add(task);
								}
							}
							
						}
					}
					
					//System.out.println("Waiting on "+waitForTheBoys.size() + " async tasks...");
					//This code is mysoginistic, hang it
					waitForTheBoys.traverse();
					
					workers.dropAll();
					csWorld.unloadUselessData().traverse();
					
					//Close region
					if (minecraftRegion != null)
						minecraftRegion.close();
					System.gc();
					
					//Display progress
					minecraftChunksImported+=32*32;
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
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		csWorld.unloadUselessData();
	}

	protected void stepTwoCreateSummaryData(WorldTool csWorld) {
		verbose("Entering step two: making summary data");

		WorldSize size = csWorld.getWorldInfo().getSize();

		int done = 0;
		int todo = (size.sizeInChunks / 8) * (size.sizeInChunks / 8);

		double completion = 0.0;
		long lastPercentageShow = System.currentTimeMillis();
		
		int wavesSize = this.threadsCount * 4;
		int wave = 0;
		
		CompoundFence compoundFence = new CompoundFence();
		for (int regionX = 0; regionX < size.sizeInChunks / 8; regionX++)
			for (int regionZ = 0; regionZ < size.sizeInChunks / 8; regionZ++)
			{
				TaskBuildRegionSummary task = new TaskBuildRegionSummary(regionX, regionZ, csWorld);
				workers.scheduleTask(task);
				compoundFence.traverse();
				
				if(wave < wavesSize) {
					
					wave++;
				}
				else
				{
					compoundFence.traverse();
					compoundFence.clear();
					
					wave = 0;
					done += wavesSize;
					
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

					//Drop all unsued chunk data
					workers.dropAll();
					
					//verbose("Saving unused chunk data...");
					csWorld.unloadUselessData().traverse();
					//verbose("Done.");
				}
			}
		compoundFence.traverse();

		//Drop all unsued chunk data
		workers.dropAll();
		
		verbose("Saving unused chunk data...");
		csWorld.unloadUselessData().traverse();
		verbose("Done.");
	}

	protected void stepThreeSpreadLightning(WorldImplementation csWorld) {
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
		
		int waveSize = this.threadsCount * 32;
		int wave = 0;

		CompoundFence waveFence = new CompoundFence();
		
		for (int chunkX = 0; chunkX < size.sizeInChunks; chunkX++)
			for (int chunkZ = 0; chunkZ < size.sizeInChunks; chunkZ++)
			{
				//if(wave < waveSize) {
					wave++;
					
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
									loadRelevantData.add(holder.waitForLoading());
									if(registeredCS_Holders.add(holder))
										chunksAquired++;
								}
							}
					
					//Wait for everything to actually load
					loadRelevantData.traverse();
	
					//Spreads lightning, from top to botton
					for (int chunkY = maxHeightPossible / 32; chunkY >= 0; chunkY--)
					{
						CubicChunk chunk = csWorld.getChunk(chunkX, chunkY, chunkZ);
						TaskLightChunk task = new TaskLightChunk(chunk, true);
						workers.scheduleTask(task);
						waveFence.add(task);
						//csWorld.getChunk(chunkX, chunkY, chunkZ).computeVoxelLightning(true);
					}
				//}
				//else
				if(wave >= waveSize)
				{
					waveFence.traverse();
					waveFence.clear();
					
					wave = 0;
					
					//Show progress
					done+= waveSize;
					if (Math.floor(((double) done / (double) todo) * 100) > completion)
					{
						completion = Math.floor(((double) done / (double) todo) * 100);
	
						if (completion >= 100.0 || (System.currentTimeMillis() - lastPercentageShow > 5000))
						{
							verbose(completion + "% ... using " + Runtime.getRuntime().freeMemory() / 1024 / 1024 + "/" + Runtime.getRuntime().maxMemory() / 1024 / 1024 + "Mb ");
							lastPercentageShow = System.currentTimeMillis();
						}
					}
				
					if (registeredCS_Holders.size() > targetChunksToKeepInRam)
					{
						//Save world
						//verbose("More than "+targetChunksToKeepInRam+" chunks already in memory, saving and unloading before continuing");
						
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
						//verbose("Done.");
					}
				}
			}

		waveFence.traverse();
		wave = 0;
		
		//Terminate
		for (ChunkHolder holder : registeredCS_Holders) {
			holder.unregisterUser(worldUser);
			chunksAquired--;
		}

		for (RegionSummary summary : registeredCS_Summaries)
			summary.unregisterUser(worldUser);

		registeredCS_Summaries.clear();
		registeredCS_Holders.clear();

		csWorld.unloadUselessData().traverse();
		
		/*csWorld.saveEverything();
		for (ChunkHolder holder : registeredCS_Holders)
			holder.unregisterUser(worldUser);

		csWorld.unloadUselessData().traverse();*/
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
	
	@Override
	public Tasks tasks() {
		return workers;
	}

}
