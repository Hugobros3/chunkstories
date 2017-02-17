package io.xol.chunkstories.renderer.chunks;

import java.util.concurrent.atomic.AtomicBoolean;

import io.xol.chunkstories.api.rendering.Primitive;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.LodLevel;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.RenderPass;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.VertexLayout;
import io.xol.engine.graphics.geometry.VertexFormat;
import io.xol.engine.graphics.geometry.VerticesObject;

public class ChunkMeshDataSections
{
	private ChunkMeshDataSections parentReference;
	private AtomicBoolean isDoneUploading = new AtomicBoolean(false);
	
	public ChunkMeshDataSections(ChunkMeshDataSections parentReference, VerticesObject verticesObject, int[][][] vertices_type_size, int[][][] vertices_type_offset)
	{
		this.parentReference = parentReference;
		this.verticesObject = verticesObject;
		this.vertices_type_size = vertices_type_size;
		this.vertices_type_offset = vertices_type_offset;
	}
	
	private final VerticesObject verticesObject;
	
	public final int[][][] vertices_type_size;
	public final int[][][] vertices_type_offset;
	
	public boolean isReady()
	{
		if(isDoneUploading.get())
			return true;
		
		if(!verticesObject.isDataPresent())
			return false;
		
		//Execute ONCE once we are done uploading the new stuff
		if(isDoneUploading.compareAndSet(false, true))
		{
			//System.out.println(this+" ready for rendering !");
			if(parentReference != null)
			{
				//System.out.println("Nulling-out parent since we are ready");
				parentReference = null;
				//Not destroying it because we have garbage collection to handle it, and we don't know if something else is clinging to it.
				//parentReference.destroy();
			}
		}
		return true;
	}
	
	public int renderSections(RenderingInterface renderingContext, LodLevel lodLevel, RenderPass renderPass)
	{
		int total = 0;
		for(VertexLayout vertexLayout : VertexLayout.values())
			total += this.renderSection(renderingContext, vertexLayout, lodLevel, renderPass);
		return total;
	}
	
	public int renderSection(RenderingInterface renderingContext, VertexLayout vertexLayout, LodLevel lodLevel, RenderPass renderPass)
	{
		//Check size isn't 0
		int size = vertices_type_size[vertexLayout.ordinal()][lodLevel.ordinal()][renderPass.ordinal()];
		if(size == 0)
			return 0;
		
		//If we aren't ready yet.
		if(!isReady())
		{
			//Check parent reference exists atomically and then go for it
			ChunkMeshDataSections parent = this.parentReference;
			if(parent != null)
				return parent.renderSection(renderingContext, vertexLayout, lodLevel, renderPass);
			
			return 0;
		}
		
		int offset = vertices_type_offset[vertexLayout.ordinal()][lodLevel.ordinal()][renderPass.ordinal()];
		
		switch(vertexLayout) {
		case WHOLE_BLOCKS:
			// Raw blocks ( integer faces coordinates ) alignment :
			// Vertex data : [VERTEX_POS(4b)][TEXCOORD(4b)][COLORS(4b)][NORMALS(4b)] Stride 16 bits
			renderingContext.bindAttribute("vertexIn", verticesObject.asAttributeSource(VertexFormat.UBYTE, 4, 16, offset + 0));
			renderingContext.bindAttribute("texCoordIn", verticesObject.asAttributeSource(VertexFormat.USHORT, 2, 16, offset + 4));
			renderingContext.bindAttribute("colorIn", verticesObject.asAttributeSource(VertexFormat.NORMALIZED_UBYTE, 4, 16, offset + 8));
			renderingContext.bindAttribute("normalIn", verticesObject.asAttributeSource(VertexFormat.U1010102, 4, 16, offset + 12));
			renderingContext.draw(Primitive.TRIANGLE, 0, size);
			return size;
		case INTRICATE:
			// Complex blocks ( fp faces coordinates ) alignment :
			// Vertex data : [VERTEX_POS(12b)][TEXCOORD(4b)][COLORS(4b)][NORMALS(4b)] Stride 24 bits
			renderingContext.bindAttribute("vertexIn", verticesObject.asAttributeSource(VertexFormat.FLOAT, 3, 24, offset + 0));
			renderingContext.bindAttribute("texCoordIn", verticesObject.asAttributeSource(VertexFormat.USHORT, 2, 24, offset + 12));
			renderingContext.bindAttribute("colorIn", verticesObject.asAttributeSource(VertexFormat.NORMALIZED_UBYTE, 4, 24, offset + 16));
			renderingContext.bindAttribute("normalIn", verticesObject.asAttributeSource(VertexFormat.U1010102, 4, 24, offset + 20));
			renderingContext.draw(Primitive.TRIANGLE, 0, size);
			return size;
		}
		
		throw new RuntimeException("Unsupported vertex layout in "+this);
	}
	
	public void destroy()
	{
		verticesObject.destroy();
	}
}
