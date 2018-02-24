//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.chunk;

import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.world.chunk.ChunkMeshDataSubtypes.ShadingType;
import io.xol.chunkstories.api.rendering.world.chunk.ChunkRenderable;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.chunk.ChunkLightUpdater;
import io.xol.chunkstories.renderer.chunks.ChunkRenderDataHolder;
import io.xol.chunkstories.renderer.chunks.ChunkRenderDataHolder.RenderLodLevel;

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
		chunkRenderData.destroy();
	}

	@Override
	public String toString()
	{
		return "[ClientChunk x:" + this.chunkX + " y:" + this.chunkY + " z:" + this.chunkZ + " air:" + isAirChunk() + " ls:" + this.lightBaker + "]";
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
		return this.lightBaker;
	}

	@Override
	public ChunkMeshUpdater meshUpdater() {
		return chunkRenderData;
	}
}
