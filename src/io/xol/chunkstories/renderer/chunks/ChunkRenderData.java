package io.xol.chunkstories.renderer.chunks;

import io.xol.chunkstories.api.rendering.Primitive;
import io.xol.chunkstories.renderer.SelectionRenderer;
import io.xol.chunkstories.renderer.chunks.ChunksRenderer.MeshedChunkData;
import io.xol.chunkstories.renderer.debug.OverlayRenderer;
import io.xol.chunkstories.world.chunk.CubicChunk;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.geometry.VertexFormat;
import io.xol.engine.graphics.geometry.VerticesObject;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Responsible of holding all rendering information about one chunk
 * ie : VBO creation, uploading and deletion, as well as decals
 */
public class ChunkRenderData
{
	public CubicChunk chunk;
	
	private VerticesObject verticesObject = new VerticesObject();
	
	public int vboSizeFullBlocks;
	public int vboSizeWaterBlocks;
	public int vboSizeCustomBlocks;
	
	public ChunkRenderData(CubicChunk chunk)
	{
		this.chunk = chunk;
	}
	
	public boolean isUploaded()
	{
		return verticesObject.isDataPresent();
	}
	
	/**
	 * Frees the ressources allocated to this ChunkRenderData
	 */
	public void free()
	{
		//Deallocate the VBO
		verticesObject.destroy();
	}
	
	/**
	 * Thread-safe way to free the ressources
	 */
	/*public void markForDeletion()
	{
		addToDeletionQueue(this);
	}*/

	/**
	 * Get the VRAM usage of this chunk in bytes
	 * @return
	 */
	/*public long getVramUsage()
	{
		return vboSizeFullBlocks * 16 + vboSizeWaterBlocks * 24 + vboSizeCustomBlocks * 24;
	}
	
	public static Set<ChunkRenderData> uselessChunkRenderDatas = ConcurrentHashMap.newKeySet();
	
	public static void deleteUselessVBOs()
	{
		Iterator<ChunkRenderData> i = uselessChunkRenderDatas.iterator();
		while(i.hasNext())
		{
			ChunkRenderData crd = i.next();
			crd.free();
			i.remove();
		}
	}
	
	public static void addToDeletionQueue(ChunkRenderData crd)
	{
		uselessChunkRenderDatas.add(crd);
	}*/

	public int renderCubeSolidBlocks(RenderingContext renderingContext)
	{
		//glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
		if (this.vboSizeFullBlocks > 0)
		{
			// We're going back to interlaced format
			// Raw blocks ( integer faces ) alignment :
			// Vertex data : [VERTEX_POS(4b)][TEXCOORD(4b)][COLORS(4b)][NORMALS(4b)] Stride 16 bits
			renderingContext.bindAttribute("vertexIn", verticesObject.asAttributeSource(VertexFormat.UBYTE, 4, 16, 0));
			renderingContext.bindAttribute("texCoordIn", verticesObject.asAttributeSource(VertexFormat.USHORT, 2, 16, 4));
			renderingContext.bindAttribute("colorIn", verticesObject.asAttributeSource(VertexFormat.NORMALIZED_UBYTE, 4, 16, 8));
			renderingContext.bindAttribute("normalIn", verticesObject.asAttributeSource(VertexFormat.U1010102, 4, 16, 12));
			renderingContext.draw(Primitive.TRIANGLE, 0, vboSizeFullBlocks);
			return vboSizeFullBlocks;
		}
		return 0;
	}
	
	public int renderCustomSolidBlocks(RenderingContext renderingContext)
	{
		if (this.vboSizeCustomBlocks > 0)
		{
			int dekal = this.vboSizeFullBlocks * 16 + this.vboSizeWaterBlocks * 24;
			// We're going back to interlaced format
			// Complex blocks ( integer faces ) alignment :
			// Vertex data : [VERTEX_POS(12b)][TEXCOORD(4b)][COLORS(4b)][NORMALS(4b)] Stride 24 bits
			renderingContext.bindAttribute("vertexIn", verticesObject.asAttributeSource(VertexFormat.FLOAT, 3, 24, dekal + 0));
			renderingContext.bindAttribute("texCoordIn", verticesObject.asAttributeSource(VertexFormat.USHORT, 2, 24, dekal + 12));
			renderingContext.bindAttribute("colorIn", verticesObject.asAttributeSource(VertexFormat.NORMALIZED_UBYTE, 4, 24, dekal + 16));
			renderingContext.bindAttribute("normalIn", verticesObject.asAttributeSource(VertexFormat.U1010102, 4, 24, dekal + 20));
			renderingContext.draw(Primitive.TRIANGLE, 0, vboSizeCustomBlocks);
			return vboSizeCustomBlocks;
		}
		return 0;
	}
	
	public int renderWaterBlocks(RenderingContext renderingContext)
	{
		if (this.vboSizeWaterBlocks > 0)
		{
			int dekal = this.vboSizeFullBlocks * 16;
			// We're going back to interlaced format
			// Complex blocks ( integer faces ) alignment :
			// Vertex data : [VERTEX_POS(12b)][TEXCOORD(4b)][COLORS(4b)][NORMALS(4b)] Stride 24 bits
			renderingContext.bindAttribute("vertexIn", verticesObject.asAttributeSource(VertexFormat.FLOAT, 3, 24, dekal + 0));
			renderingContext.bindAttribute("texCoordIn", verticesObject.asAttributeSource(VertexFormat.USHORT, 2, 24, dekal + 12));
			renderingContext.bindAttribute("colorIn", verticesObject.asAttributeSource(VertexFormat.NORMALIZED_UBYTE, 4, 24, dekal + 16));
			renderingContext.bindAttribute("normalIn", verticesObject.asAttributeSource(VertexFormat.U1010102, 4, 24, dekal + 20));
			renderingContext.draw(Primitive.TRIANGLE, 0, vboSizeWaterBlocks);
			return vboSizeWaterBlocks;
		}
		return 0;
	}
	
	public void renderChunkBounds(RenderingContext renderingContext)
	{
		//if(chunk.chunkZ != 5)
		//	return;
		OverlayRenderer.glColor4f(5, 0, (float) Math.random() * 0.01f, 1);
		SelectionRenderer.cubeVertices(chunk.getChunkX() * 32 + 16, chunk.getChunkY() * 32, chunk.getChunkZ() * 32 + 16, 32, 32, 32);
	}

	public void setChunkMeshes(MeshedChunkData mcd)
	{
		verticesObject.uploadData(mcd.buffer);
		
		this.vboSizeFullBlocks = mcd.solidVoxelsSize;
		this.vboSizeCustomBlocks = mcd.solidModelsSize;
		this.vboSizeWaterBlocks = mcd.waterModelsSize;
	}
}
