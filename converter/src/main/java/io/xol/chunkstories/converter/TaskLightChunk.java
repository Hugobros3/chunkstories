package io.xol.chunkstories.converter;

import io.xol.chunkstories.api.workers.Task;
import io.xol.chunkstories.api.workers.TaskExecutor;
import io.xol.chunkstories.world.chunk.CubicChunk;

public class TaskLightChunk extends Task {

	CubicChunk chunk;
	boolean adjacent;
	
	public TaskLightChunk(CubicChunk chunk, boolean adjacent) {
		super();
		this.chunk = chunk;
		this.adjacent = adjacent;
	}

	@Override
	protected boolean task(TaskExecutor taskExecutor) {
		chunk.computeVoxelLightning(adjacent);
		return true;
	}

}
