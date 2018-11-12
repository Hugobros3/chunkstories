//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.io;

import io.xol.chunkstories.api.workers.TaskExecutor;
import io.xol.chunkstories.world.chunk.ChunkHolderImplementation;
import io.xol.chunkstories.world.chunk.CompressedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IOTaskLoadChunk extends IOTask {
	ChunkHolderImplementation chunkSlot;

	public IOTaskLoadChunk(ChunkHolderImplementation chunkSlot) {
		this.chunkSlot = chunkSlot;
	}

	static Logger logger = LoggerFactory.getLogger("world.chunkIO");

	@Override
	public boolean task(TaskExecutor taskExecutor) {
		// When a loader was removed from the world, remaining operations on it are discarded
		if (chunkSlot.getRegion().isUnloaded())
			return true;

		// And so are redudant operations
		if (chunkSlot.isChunkLoaded())
			return true;

		// If for some reason the chunks holder's are still not loaded, we requeue the job
		if (!chunkSlot.getRegion().isDiskDataLoaded())
			return false;

		if(chunkSlot.loadChunkTask == null) {
			logger.error("Task to load chunk seem be useless! ");
		} else if(chunkSlot.loadChunkTask != this) {
			logger.error("Task to load chunk seem obsolete !");
		}

		CompressedData compressedData = chunkSlot.getCompressedData();
		chunkSlot.receiveDataAndCreate(compressedData);

		return true;
	}

	@Override
	public String toString() {
		return "[IOTaskLoadChunk " + chunkSlot + "]";
	}
}