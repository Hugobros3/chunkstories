package io.xol.chunkstories.world.chunk;

import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.world.ChunkRenderable;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.ShadingType;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.chunk.ChunkLightUpdater;
import io.xol.chunkstories.renderer.chunks.ChunkRenderDataHolder;
import io.xol.chunkstories.renderer.chunks.ChunkRenderDataHolder.RenderLodLevel;
import io.xol.chunkstories.world.chunk.ChunkHolderImplementation;
import io.xol.chunkstories.world.chunk.CompressedData;
import io.xol.chunkstories.world.chunk.CubicChunk;

public class ClientChunk extends CubicChunk implements ChunkRenderable {

	// Used in client rendering
	public final ChunkRenderDataHolder chunkRenderData;

	//public AtomicBoolean need_render = new AtomicBoolean(true);
	//public AtomicBoolean need_render_fast = new AtomicBoolean(false);
	//public AtomicBoolean requestable = new AtomicBoolean(true);
	
	public ClientChunk(ChunkHolderImplementation holder, int chunkX, int chunkY, int chunkZ) {
		super(holder, chunkX, chunkY, chunkZ);
		
		assert world instanceof WorldClient;
		
		this.chunkRenderData = new ChunkRenderDataHolder(this, ((WorldClient)world).getWorldRenderer());
	}

	public ClientChunk(ChunkHolderImplementation holder, int chunkX, int chunkY, int chunkZ, CompressedData data) {
		super(holder, chunkX, chunkY, chunkZ, data);
		
		assert world instanceof WorldClient;
		
		this.chunkRenderData = new ChunkRenderDataHolder(this, ((WorldClient)world).getWorldRenderer());
	}

	@Override
	public void destroy()
	{
		super.destroy();
		if(chunkRenderData != null)
			chunkRenderData.destroy();
	}

	@Override
	public String toString()
	{
		return "[ClientChunk x:" + this.chunkX + " y:" + this.chunkY + " z:" + this.chunkZ + " air:" + isAirChunk() + " ls:" + this.lightBakingStatus + "]";
	}
	
	public ChunkRenderDataHolder getChunkRenderData()
	{
		return chunkRenderData;
	}

	public int renderPass(RenderingInterface renderingInterface, RenderLodLevel renderLodLevel, ShadingType renderPass)
	{
		return chunkRenderData.renderPass(renderingInterface, renderLodLevel, renderPass);
	}

	@Override
	public ChunkLightUpdater lightBaker() {
		return this.lightBakingStatus;
	}

	@Override
	public ChunkMeshUpdater meshUpdater() {
		return chunkRenderData;
	}
}
