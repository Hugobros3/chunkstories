package io.xol.chunkstories.renderer.chunks;

import io.xol.chunkstories.api.rendering.world.ChunkRenderable;
import io.xol.chunkstories.workers.TasksPool;
import io.xol.chunkstories.world.chunk.CubicChunk;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ChunkMeshesBakerPool extends TasksPool<TaskBakeChunk> implements ChunkMeshesBaker{

	ThreadLocal<ChunkMeshingData> localData = new ThreadLocal<ChunkMeshingData>() {

		@Override
		protected ChunkMeshingData initialValue() {
			return new ChunkMeshingData();
		}
		
	};
	
	@Override
	public void requestChunkRender(ChunkRenderable chunk) {
		
		if(chunk == null)
			throw new NullPointerException();
		
		TaskBakeChunk task = new TaskBakeChunk(this, (CubicChunk)chunk);
		
		this.scheduleTask(task);
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}
	
	class ChunkMeshingData {
		
	}

}
