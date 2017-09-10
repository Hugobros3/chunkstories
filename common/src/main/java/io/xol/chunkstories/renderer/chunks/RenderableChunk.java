package io.xol.chunkstories.renderer.chunks;

import java.util.concurrent.atomic.AtomicBoolean;

import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.world.ChunkRenderable;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.ShadingType;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.renderer.chunks.ChunkRenderDataHolder.RenderLodLevel;
import io.xol.chunkstories.world.chunk.ChunkHolderImplementation;
import io.xol.chunkstories.world.chunk.CubicChunk;

public class RenderableChunk extends CubicChunk implements ChunkRenderable {

	// Used in client rendering
	private final ChunkRenderDataHolder chunkRenderData;

	public AtomicBoolean need_render = new AtomicBoolean(true);
	public AtomicBoolean need_render_fast = new AtomicBoolean(false);
	public AtomicBoolean requestable = new AtomicBoolean(true);
	
	public RenderableChunk(ChunkHolderImplementation holder, int chunkX, int chunkY, int chunkZ) {
		super(holder, chunkX, chunkY, chunkZ);
		
		if(world instanceof WorldClient)
			this.chunkRenderData = new ChunkRenderDataHolder(this, ((WorldClient)world).getWorldRenderer());
		else
			this.chunkRenderData = null;
	}

	public RenderableChunk(ChunkHolderImplementation holder, int chunkX, int chunkY, int chunkZ, int[] data) {
		super(holder, chunkX, chunkY, chunkZ, data);
		
		if(world instanceof WorldClient)
			this.chunkRenderData = new ChunkRenderDataHolder(this, ((WorldClient)world).getWorldRenderer());
		else
			this.chunkRenderData = null;
	}
	
	@Override
	public void markForReRender()
	{
		this.need_render.set(true);
	}

	@Override
	public boolean isMarkedForReRender()
	{
		return this.need_render.get();
	}

	@Override
	public void markRenderInProgress(boolean inProgress)
	{
		this.requestable.set(!inProgress);
	}

	@Override
	public boolean isRenderAleadyInProgress()
	{
		return !requestable.get();
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
		return "[RenderableChunk x:" + this.chunkX + " y:" + this.chunkY + " z:" + this.chunkZ + " air:" + isAirChunk() + " nl:" + this.needRelightning + " nr:" + need_render.get() + " ip: " + this.isRenderAleadyInProgress() + "]";
	}
	
	public ChunkRenderDataHolder getChunkRenderData()
	{
		return chunkRenderData;
	}

	public int renderPass(RenderingInterface renderingInterface, RenderLodLevel renderLodLevel, ShadingType renderPass)
	{
		return chunkRenderData.renderPass(renderingInterface, renderLodLevel, renderPass);
	}
}
