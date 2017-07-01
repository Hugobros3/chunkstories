package io.xol.chunkstories.renderer.chunks;

import io.xol.chunkstories.workers.Task;
import io.xol.chunkstories.world.chunk.CubicChunk;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class TaskBakeChunk extends Task {

	private final ChunkMeshesBakerPool baker;
	private final CubicChunk chunk;
	
	public TaskBakeChunk(ChunkMeshesBakerPool baker, CubicChunk chunk) {
		super();
		this.baker = baker;
		this.chunk = chunk;
	}

	@Override
	protected boolean task() {
		return false;
	}

}
