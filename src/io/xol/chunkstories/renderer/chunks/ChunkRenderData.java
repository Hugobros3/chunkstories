package io.xol.chunkstories.renderer.chunks;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.xol.chunkstories.api.rendering.Primitive;
import io.xol.chunkstories.renderer.SelectionRenderer;
import io.xol.chunkstories.renderer.buffers.ByteBufferPool;
import io.xol.chunkstories.renderer.buffers.ByteBufferPool.RecyclableByteBuffer;
import io.xol.chunkstories.renderer.debug.OverlayRenderer;
import io.xol.chunkstories.world.chunk.CubicChunk;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.geometry.VertexFormat;
import io.xol.engine.graphics.geometry.VerticesObject;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Responsible of holding all rendering information about one chunk
 * ie : VBO creation, uploading and deletion, as well as decals
 */
public class ChunkRenderData
{
	public CubicChunk chunk;
	
	VerticesObject verticesObject = new VerticesObject();
	//public int vboId = -1;
	
	public int vboSizeFullBlocks;
	public int vboSizeWaterBlocks;
	public int vboSizeCustomBlocks;
	
	//public int byteBufferPoolId = -1;
	
	public boolean isUploaded = false;
	
	public ChunkRenderData(CubicChunk chunk)
	{
		this.chunk = chunk;
	}
	
	public boolean isUploaded()
	{
		return verticesObject.isDataPresent();
	}
	
	/**
	 * Uploads the ByteBuffer contents and frees it
	 */
	/*public void upload()
	{
		verticesObject.uploadData(pool.accessByteBuffer(byteBufferPoolId));
		
		//Release BB
		pool.releaseByteBuffer(byteBufferPoolId);
		byteBufferPoolId = -1;
		
		isUploaded = true;
	}
	*/
	
	/**
	 * Frees the ressources allocated to this ChunkRenderData
	 */
	public void free()
	{
		//Make sure we freed the byteBuffer
		/*-if(byteBufferPoolId != -1)
			pool.releaseByteBuffer(byteBufferPoolId);
		byteBufferPoolId = -1;*/
		
		//Deallocate the VBO
		verticesObject.destroy();
	}
	
	/**
	 * Thread-safe way to free the ressources
	 */
	public void markForDeletion()
	{
		addToDeletionQueue(this);
	}

	/**
	 * Get the VRAM usage of this chunk in bytes
	 * @return
	 */
	public long getVramUsage()
	{
		return vboSizeFullBlocks * 16 + vboSizeWaterBlocks * 24 + vboSizeCustomBlocks * 24;
	}
	
	// End class instance code, begin static de-allocation functions
	
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
	}

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
			
			/*renderingContext.setVertexAttributePointerLocation("vertexIn", 4, GL_UNSIGNED_BYTE, false, 16, 0, verticesObject);
			renderingContext.setVertexAttributePointerLocation("texCoordIn", 2, GL_UNSIGNED_SHORT, false, 16, 4, verticesObject);
			renderingContext.setVertexAttributePointerLocation("colorIn", 4, GL_UNSIGNED_BYTE, true, 16, 8, verticesObject);
			renderingContext.setVertexAttributePointerLocation("normalIn", 4, GL_UNSIGNED_INT_2_10_10_10_REV, true, 16, 12, verticesObject);
			*/
			//verticesObject.drawElementsTriangles(vboSizeFullBlocks);
			//glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
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
			
			/*renderingContext.setVertexAttributePointerLocation("vertexIn", 3, GL_FLOAT, false, 24, dekal + 0, verticesObject);
			renderingContext.setVertexAttributePointerLocation("texCoordIn", 2, GL_UNSIGNED_SHORT, false, 24, dekal + 12, verticesObject);
			renderingContext.setVertexAttributePointerLocation("colorIn", 4, GL_UNSIGNED_BYTE, true, 24, dekal + 16, verticesObject);
			renderingContext.setVertexAttributePointerLocation("normalIn", 4, GL_UNSIGNED_INT_2_10_10_10_REV, true, 24, dekal + 20, verticesObject);
			verticesObject.drawElementsTriangles(vboSizeCustomBlocks);
			return vboSizeCustomBlocks;*/
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
			/*renderingContext.setVertexAttributePointerLocation("vertexIn", 3, GL_FLOAT, false, 24, dekal + 0, verticesObject);
			renderingContext.setVertexAttributePointerLocation("texCoordIn", 2, GL_UNSIGNED_SHORT, false, 24, dekal + 12, verticesObject);
			renderingContext.setVertexAttributePointerLocation("colorIn", 4, GL_UNSIGNED_BYTE, true, 24, dekal + 16, verticesObject);
			renderingContext.setVertexAttributePointerLocation("normalIn", 4, GL_UNSIGNED_INT_2_10_10_10_REV, true, 24, dekal + 20, verticesObject);
			verticesObject.drawElementsTriangles(vboSizeWaterBlocks);*/
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

	public void setChunkMeshes(RecyclableByteBuffer buffer)
	{
		verticesObject.uploadData(buffer);
	}
}
