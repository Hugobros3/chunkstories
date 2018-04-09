package io.xol.chunkstories.world.io;

import io.xol.chunkstories.api.workers.TaskExecutor;
import io.xol.chunkstories.api.world.region.Region;
import io.xol.chunkstories.world.chunk.ChunkHolderImplementation;
import io.xol.chunkstories.world.chunk.CompressedData;
import io.xol.chunkstories.world.chunk.CubicChunk;

public class IOTaskLoadChunk extends IOTask {
	ChunkHolderImplementation chunkSlot;

	public IOTaskLoadChunk(ChunkHolderImplementation chunkSlot)
	{
		this.chunkSlot = chunkSlot;
	}

	@Override
	public boolean task(TaskExecutor taskExecutor)
	{
		// When a loader was removed from the world, remaining operations on it are discarded
		if (chunkSlot.getRegion().isUnloaded())
			return true;
		
		// And so are redudant operations
		if (chunkSlot.isChunkLoaded())
			return true;

		/*Region region = chunkSlot.getRegion();
		Region actualRegion = world.getRegion(region.getRegionX(), region.getRegionY(), region.getRegionZ());
		
		if(region != actualRegion) {
			System.out.println("Some quircky race condition led to this region being discarded then loaded again but without raising the isUnloaded() flag !");
			System.out.println(region + " vs: " + actualRegion);
			return true;
		}*/
		
		// If for some reason the chunks holder's are still not loaded, we requeue the job
		if (!chunkSlot.getRegion().isDiskDataLoaded())
			return false;

		CompressedData compressedData = chunkSlot.getCompressedData();
		//Not yet generated chunk; call the generator
		if (compressedData == null) {
			/*Chunk chunk = */chunkSlot.createChunk();
			//world.getGenerator().generateChunk(chunk);
		}
		//Normal voxel data is present, uncompressed it then load it to the chunk
		else {
			CubicChunk chunk = chunkSlot.createChunk(compressedData);
			//Postprocess ?
		}

		//chunkSlot.setChunk(result);
		/*synchronized (chunkSlot)
		{
			chunkSlot.notifyAll();
		}*/
		return true;
	}

	@Override
	public String toString()
	{
		return "[IOTaskLoadChunk " + chunkSlot + "]";
	}
}