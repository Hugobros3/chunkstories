package io.xol.chunkstories.converter;

import io.xol.chunkstories.anvil.MinecraftChunk;
import io.xol.chunkstories.anvil.MinecraftRegion;
import io.xol.chunkstories.api.workers.Task;
import io.xol.chunkstories.api.workers.TaskExecutor;
import io.xol.chunkstories.api.world.chunk.ChunkHolder;
import io.xol.chunkstories.converter.ConverterMapping.Mapper;
import io.xol.chunkstories.converter.ConverterMapping.NonTrivialMapper;
import io.xol.chunkstories.converter.ConverterWorkers.ConverterWorkerThread;
import io.xol.chunkstories.tools.WorldTool;
import io.xol.engine.concurrency.CompoundFence;

public class TaskConvertMcChunk extends Task {

	MinecraftRegion minecraftRegion;
	
	private int chunkStoriesCurrentChunkX;
	private int chunkStoriesCurrentChunkZ;
	
	private int minecraftCurrentChunkXinsideRegion;
	private int minecraftCuurrentChunkZinsideRegion;

	private int minecraftRegionX;
	private int minecraftRegionZ;

	private ConverterMapping mappers;

	private MinecraftChunk minecraftChunk;
	
	public TaskConvertMcChunk(MinecraftRegion minecraftRegion, MinecraftChunk minecraftChunk, int chunkStoriesCurrentChunkX,
			int chunkStoriesCurrentChunkZ, int minecraftCurrentChunkXinsideRegion,
			int minecraftCuurrentChunkZinsideRegion, int minecraftRegionX, int minecraftRegionZ,
			ConverterMapping quickConversion) {
		this.minecraftRegion = minecraftRegion;
		this.minecraftChunk = minecraftChunk;
		
		this.chunkStoriesCurrentChunkX = chunkStoriesCurrentChunkX;
		this.chunkStoriesCurrentChunkZ = chunkStoriesCurrentChunkZ;
		this.minecraftCurrentChunkXinsideRegion = minecraftCurrentChunkXinsideRegion;
		this.minecraftCuurrentChunkZinsideRegion = minecraftCuurrentChunkZinsideRegion;
		this.minecraftRegionX = minecraftRegionX;
		this.minecraftRegionZ = minecraftRegionZ;
		this.mappers = quickConversion;
	}

	@Override
	protected boolean task(TaskExecutor taskExecutor) {
		
		ConverterWorkerThread cwt = (ConverterWorkerThread)taskExecutor;
		WorldTool csWorld = cwt.world();
		
		//Is it within our borders ?
		
		//if (chunkStoriesCurrentChunkX >= 0 && chunkStoriesCurrentChunkX < cwt.size().sizeInChunks * 32 && chunkStoriesCurrentChunkZ >= 0 && chunkStoriesCurrentChunkZ < cwt.size().sizeInChunks * 32)
		{
			//Load the chunk
			//MinecraftChunk minecraftChunk = null;
			try
			{
				//Tries loading the Minecraft chunk

				if (minecraftChunk != null)
				{
					//If it succeed, we first require to load the corresponding chunkstories stuff

					//Ignore the summaries for now
					
					/*RegionSummary summary = csWorld.getRegionsSummariesHolder().aquireRegionSummaryWorldCoordinates(this, chunkStoriesCurrentChunkX, chunkStoriesCurrentChunkZ);
					if(summary != null)
						registeredCS_Summaries.add(summary);*/

					CompoundFence loadRelevantData = new CompoundFence();
					
					//Then the chunks
					for (int y = 0; y < OfflineWorldConverter.mcWorldHeight; y += 32)
					{
						ChunkHolder holder = csWorld.aquireChunkHolderWorldCoordinates(cwt, chunkStoriesCurrentChunkX, y, chunkStoriesCurrentChunkZ);
						if (holder != null) {
							loadRelevantData.add(holder.waitForLoading());
							
							if(cwt.registeredCS_Holders.add(holder))
								cwt.chunksAquired++;
						}
					}
					
					//Wait for them to actually load
					loadRelevantData.traverse();

					for (int x = 0; x < 16; x++)
						for (int z = 0; z < 16; z++)
							for (int y = 0; y < OfflineWorldConverter.mcWorldHeight; y++)
							{
								//Translate each block
								int mcId = minecraftChunk.getBlockID(x, y, z) & 0xFFF;
								byte meta = (byte) (minecraftChunk.getBlockMeta(x, y, z) & 0xF);
								
								//Ignore air blocks
								if (mcId != 0)
								{
									Mapper mapper = this.mappers.getMapper(mcId, meta);
									if(mapper == null)
										continue;
									
									if(mapper instanceof NonTrivialMapper) {
										((NonTrivialMapper)mapper).output(csWorld, chunkStoriesCurrentChunkX + x, y, chunkStoriesCurrentChunkZ + z, mcId, meta, minecraftRegion, minecraftCurrentChunkXinsideRegion, minecraftCuurrentChunkZinsideRegion, x, y, z);
									} else {
										
										//Directly set trivial blocks
										int trivial = mapper.output(mcId, meta);
										if(trivial != 0x0) {
											csWorld.pokeSimpleSilently(chunkStoriesCurrentChunkX + x, y, chunkStoriesCurrentChunkZ + z, trivial);
										}
									}
									
									
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
								}
							}

					//Converts external data such as signs
					SpecialBlocksHandler.processAdditionalStuff(minecraftChunk, csWorld, chunkStoriesCurrentChunkX, 0, chunkStoriesCurrentChunkZ);
				}
			}
			catch (Exception e)
			{
				cwt.converter().verbose("Issue with chunk " + minecraftCurrentChunkXinsideRegion + " " + minecraftCuurrentChunkZinsideRegion + " of region " + minecraftRegionX + " " + minecraftRegionZ + ".");
				e.printStackTrace();
			}

			//Display progress
			/*minecraftChunksImported++;
			if (Math.floor(((double) minecraftChunksImported / (double) minecraftChunksToImport) * 100) > completion)
			{
				completion = Math.floor(((double) minecraftChunksImported / (double) minecraftChunksToImport) * 100);

				if (completion >= 100.0 || (System.currentTimeMillis() - lastPercentageShow > 5000))
				{
					verbose(completion + "% ... (" + csWorld.getRegionsHolder().countChunks() + " chunks loaded ) using " + Runtime.getRuntime().freeMemory() / 1024 / 1024 + "/" + Runtime.getRuntime().maxMemory() / 1024 / 1024
							+ "Mb ");
					lastPercentageShow = System.currentTimeMillis();
				}
			}*/
		}
		
		return true;
	}

}
