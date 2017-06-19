package io.xol.chunkstories.renderer.chunks;

import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.LodLevel;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.ShadingType;
import io.xol.chunkstories.renderer.SelectionRenderer;
import io.xol.chunkstories.renderer.debug.FakeImmediateModeDebugRenderer;
import io.xol.chunkstories.world.chunk.CubicChunk;
import io.xol.engine.graphics.RenderingContext;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Responsible of holding all rendering information about one chunk
 * ie : VBO creation, uploading and deletion, as well as decals
 */
public class ChunkRenderDataHolder
{
	public CubicChunk chunk;
	
	private ChunkMeshDataSections data;
	/*private VerticesObject verticesObject = new VerticesObject();
	
	public int vboSizeFullBlocks;
	public int vboSizeWaterBlocks;
	public int vboSizeCustomBlocks;*/
	
	public ChunkRenderDataHolder(CubicChunk chunk)
	{
		this.chunk = chunk;
	}
	
	/**
	 * Frees the ressources allocated to this ChunkRenderData
	 */
	public void free()
	{
		data = null;
		//Deallocate the VBO
		//verticesObject.destroy();
	}

	/*public int renderCubeSolidBlocks(RenderingContext renderingContext)
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
	}*/
	
	public void renderChunkBounds(RenderingContext renderingContext)
	{
		//if(chunk.chunkZ != 5)
		//	return;
		FakeImmediateModeDebugRenderer.glColor4f(5, 0, (float) Math.random() * 0.01f, 1);
		SelectionRenderer.cubeVertices(chunk.getChunkX() * 32 + 16, chunk.getChunkY() * 32, chunk.getChunkZ() * 32 + 16, 32, 32, 32);
	}

	public ChunkMeshDataSections getData()
	{
		return data;
	}

	public void setData(ChunkMeshDataSections data)
	{
		if(data == null)
			throw new NullPointerException("setData() requires non-null ata");
		this.data = data;
	}

	public int renderPass(RenderingInterface renderingInterface, RenderLodLevel renderLodLevel, ShadingType shadingType)
	{
		ChunkMeshDataSections data = this.data;
		if(data == null)
			return 0;
		
		switch(renderLodLevel) {
		case HIGH:
			return data.renderSections(renderingInterface, LodLevel.ANY, shadingType) + data.renderSections(renderingInterface, LodLevel.HIGH, shadingType);
		case LOW:
			return data.renderSections(renderingInterface, LodLevel.ANY, shadingType) + data.renderSections(renderingInterface, LodLevel.LOW, shadingType);
		}
		
		throw new RuntimeException("Undefined switch() case for RenderLodLevel "+renderLodLevel);
	}
	
	public enum RenderLodLevel {
		HIGH,
		LOW
	}
}
