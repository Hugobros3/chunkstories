//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.converter;

import io.xol.chunkstories.api.converter.mappings.Mapper;
import io.xol.chunkstories.api.converter.mappings.NonTrivialMapper;
import io.xol.chunkstories.api.workers.Task;
import io.xol.chunkstories.api.workers.TaskExecutor;
import io.xol.chunkstories.api.world.cell.FutureCell;
import io.xol.chunkstories.api.world.chunk.ChunkHolder;
import io.xol.chunkstories.converter.ConverterWorkers.ConverterWorkerThread;
import io.xol.chunkstories.util.concurrency.CompoundFence;
import io.xol.chunkstories.world.WorldTool;
import io.xol.enklume.MinecraftChunk;
import io.xol.enklume.MinecraftRegion;

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
										FutureCell future = new FutureCell(csWorld, chunkStoriesCurrentChunkX + x, y, chunkStoriesCurrentChunkZ + z, csWorld.getContent().voxels().air());
										
										//Directly set trivial blocks
										mapper.output(mcId, meta, future);
										if(!future.getVoxel().isAir())
											csWorld.pokeSimpleSilently(future);
									}
									
								}
							}

					//Converts external data such as signs
					//SpecialBlocksHandler.processAdditionalStuff(minecraftChunk, csWorld, chunkStoriesCurrentChunkX, 0, chunkStoriesCurrentChunkZ);
				}
			}
			catch (Exception e)
			{
				cwt.converter().verbose("Issue with chunk " + minecraftCurrentChunkXinsideRegion + " " + minecraftCuurrentChunkZinsideRegion + " of region " + minecraftRegionX + " " + minecraftRegionZ + ".");
				e.printStackTrace();
			}
		}
		
		return true;
	}

}
