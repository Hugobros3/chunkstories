//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.io;

import io.xol.chunkstories.api.workers.Task;
import io.xol.chunkstories.api.workers.TaskExecutor;
import io.xol.chunkstories.world.storage.ChunkHolderImplementation;
import io.xol.chunkstories.world.chunk.CompressedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskLoadChunk extends Task {
	ChunkHolderImplementation chunkSlot;

	public TaskLoadChunk(ChunkHolderImplementation chunkSlot) {
		this.chunkSlot = chunkSlot;
	}

	@Override
	public boolean task(TaskExecutor taskExecutor) {
		CompressedData compressedData = chunkSlot.getCompressedData();
		chunkSlot.receiveDataAndCreate(compressedData);
		return true;
	}

	@Override
	public String toString() {
		return "[TaskLoadChunk " + chunkSlot + "]";
	}
}