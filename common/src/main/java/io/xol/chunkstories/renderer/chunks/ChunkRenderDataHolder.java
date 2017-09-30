package io.xol.chunkstories.renderer.chunks;

import java.util.concurrent.Semaphore;

import io.xol.chunkstories.api.rendering.Primitive;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.WorldRenderer;
import io.xol.chunkstories.api.rendering.vertex.VertexBuffer;
import io.xol.chunkstories.api.rendering.vertex.VertexFormat;
import io.xol.chunkstories.api.util.concurrency.Fence;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.LodLevel;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.ShadingType;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.VertexLayout;
import io.xol.chunkstories.world.chunk.CubicChunk;
import io.xol.engine.concurrency.SimpleFence;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Responsible of holding all rendering information about one chunk
 * ie : VBO creation, uploading and deletion, as well as decals
 */
public class ChunkRenderDataHolder
{
	private final CubicChunk chunk;
	private final WorldRenderer worldRenderer;
	
	private ChunkMeshDataSections currentData;
	
	//ConcurrentLinkedDeque<ChunkMeshDataSections> pendingUpload = new ConcurrentLinkedDeque<ChunkMeshDataSections>();
	
	boolean isDestroyed = false;
	
	private Semaphore noDrawDeleteConflicts = new Semaphore(1);
	private Semaphore oneUploadAtATime = new Semaphore(1);
	protected VertexBuffer verticesObject = null;
	
	protected ChunkMeshDataSections pleaseUploadMe = null;
	protected SimpleFence pleaseUploadMeFence = null;
	
	public ChunkRenderDataHolder(CubicChunk chunk, WorldRenderer worldRenderer)
	{
		this.chunk = chunk;
		this.worldRenderer = worldRenderer;
	}
	
	public CubicChunk getChunk() {
		return chunk;
	}
	
	/**
	 * Frees the ressources allocated to this ChunkRenderData
	 */
	public void destroy()
	{
		noDrawDeleteConflicts.acquireUninterruptibly();
		
		isDestroyed = true;
		
		currentData = null;
		//Deallocate the VBO
		if(verticesObject != null)
			verticesObject.destroy();
		
		noDrawDeleteConflicts.release();
	}
	
	/*public void renderChunkBounds(RenderingContext renderingContext)
	{
		//if(chunk.chunkZ != 5)
		//	return;
		FakeImmediateModeDebugRenderer.glColor4f(5, 0, (float) Math.random() * 0.01f, 1);
		SelectionRenderer.cubeVertices(chunk.getChunkX() * 32 + 16, chunk.getChunkY() * 32, chunk.getChunkZ() * 32 + 16, 32, 32, 32);
	}*/

	public boolean isDataAvailable()
	{
		return currentData != null;
	}

	public void setData(ChunkMeshDataSections newData)
	{
		if(newData == null)
			throw new NullPointerException("setData() requires non-null ata");
		
		oneUploadAtATime.acquireUninterruptibly();
		noDrawDeleteConflicts.acquireUninterruptibly();
		
		//Meh that's a waste of time then
		if(isDestroyed) {
			noDrawDeleteConflicts.release();
			oneUploadAtATime.release();
			newData.notNeeded(); //<-- Free the data
			return;
		}
		
		//currentData = data;
		
		//No verticesObject already created; create one, fill it and then change the bails
		if(verticesObject == null) {
			VertexBuffer wip = worldRenderer.getRenderingInterface().newVertexBuffer();
			Fence fence = wip.uploadData(newData.dataToUpload);
			
			//We unlock while waiting for the upload
			noDrawDeleteConflicts.release();
			fence.traverse();
			
			//Then we lock again
			noDrawDeleteConflicts.acquireUninterruptibly();
			verticesObject = wip;
			currentData = newData;
			
			//And we're good !
		}
		//Already a VerticesObject present hum, we create another one then delete the old one
		else {
			VertexBuffer wip = worldRenderer.getRenderingInterface().newVertexBuffer();
			Fence fence = wip.uploadData(newData.dataToUpload);
			
			//We unlock while waiting for the upload
			noDrawDeleteConflicts.release();
			fence.traverse();
			
			//Then we lock again
			noDrawDeleteConflicts.acquireUninterruptibly();
			
			//We delete the OLD one
			verticesObject.destroy();
			
			//We swap the new one in
			verticesObject = wip;
			currentData = newData;
		}
		
		newData.consumed();
		
		noDrawDeleteConflicts.release();
		oneUploadAtATime.release();
	}

	public int renderPass(RenderingInterface renderingInterface, RenderLodLevel renderLodLevel, ShadingType shadingType)
	{
		try {
			noDrawDeleteConflicts.acquireUninterruptibly();
			
			if(currentData == null)
				return 0;
			
			/*switch(renderLodLevel) {
			case HIGH:
				return renderSections(renderingInterface, LodLevel.ANY, shadingType) + renderSections(renderingInterface, LodLevel.HIGH, shadingType);
			case LOW:
				return renderSections(renderingInterface, LodLevel.ANY, shadingType) + renderSections(renderingInterface, LodLevel.LOW, shadingType);
			}*/
			
			return renderSections(renderingInterface, renderLodLevel, shadingType);
			
			//throw new RuntimeException("Undefined switch() case for RenderLodLevel "+renderLodLevel);
		}
		finally {
			noDrawDeleteConflicts.release();
		}
	}
	
	/** Render the lodLevel+shading type combination using any VertexLayout */
	public int renderSections(RenderingInterface renderingContext, RenderLodLevel renderLodLevel, ShadingType renderPass)
	{
		int total = 0;
		for(VertexLayout vertexLayout : VertexLayout.values())
			total += this.renderSection(renderingContext, vertexLayout, renderLodLevel, renderPass);
		return total;
	}
	
	int array[] = new int[4];
	
	public int renderSection(RenderingInterface renderingContext, VertexLayout vertexLayout, RenderLodLevel renderLodLevel, ShadingType renderPass)
	{
		LodLevel any = LodLevel.ANY;
		LodLevel also = renderLodLevel.equals(RenderLodLevel.HIGH) ? LodLevel.HIGH : LodLevel.LOW; 
		
		//Check size isn't 0
		int sizeAny = currentData.vertices_type_size[vertexLayout.ordinal()][any.ordinal()][renderPass.ordinal()];
		int sizeAlso = currentData.vertices_type_size[vertexLayout.ordinal()][also.ordinal()][renderPass.ordinal()];
		
		if(sizeAny + sizeAlso == 0)
			return 0;
		
		int offsetAny = currentData.vertices_type_offset[vertexLayout.ordinal()][any.ordinal()][renderPass.ordinal()];
		int offsetAlso = currentData.vertices_type_offset[vertexLayout.ordinal()][also.ordinal()][renderPass.ordinal()];

		int offset = 0;
		
		if(offsetAlso < offsetAny)
			System.out.println("prout.");
		
		offset = offsetAny;
		offsetAny = 0;
		offsetAlso -= offsetAny;
		
		switch(vertexLayout) {
		case WHOLE_BLOCKS:
			// Raw blocks ( integer faces coordinates ) alignment :
			// Vertex data : [VERTEX_POS(4b)][TEXCOORD(4b)][COLORS(4b)][NORMALS(4b)] Stride 16 bits
			renderingContext.bindAttribute("vertexIn", verticesObject.asAttributeSource(VertexFormat.UBYTE, 4, 16, offset + 0));
			renderingContext.bindAttribute("texCoordIn", verticesObject.asAttributeSource(VertexFormat.USHORT, 2, 16, offset + 4));
			renderingContext.bindAttribute("colorIn", verticesObject.asAttributeSource(VertexFormat.NORMALIZED_UBYTE, 4, 16, offset + 8));
			renderingContext.bindAttribute("normalIn", verticesObject.asAttributeSource(VertexFormat.U1010102, 4, 16, offset + 12));
			//renderingContext.draw(Primitive.TRIANGLE, 0, size);
			//return size; 
			break;
		case INTRICATE:
			// Complex blocks ( fp faces coordinates ) alignment :
			// Vertex data : [VERTEX_POS(12b)][TEXCOORD(4b)][COLORS(4b)][NORMALS(4b)] Stride 24 bits
			renderingContext.bindAttribute("vertexIn", verticesObject.asAttributeSource(VertexFormat.FLOAT, 3, 24, offset + 0));
			renderingContext.bindAttribute("texCoordIn", verticesObject.asAttributeSource(VertexFormat.USHORT, 2, 24, offset + 12));
			renderingContext.bindAttribute("colorIn", verticesObject.asAttributeSource(VertexFormat.NORMALIZED_UBYTE, 4, 24, offset + 16));
			renderingContext.bindAttribute("normalIn", verticesObject.asAttributeSource(VertexFormat.U1010102, 4, 24, offset + 20));
			//renderingContext.draw(Primitive.TRIANGLE, 0, size);
			//return size;
			break;
		default:
			throw new RuntimeException("Unsupported vertex layout in "+this);
		}
		
		if(sizeAlso == 0) {
			renderingContext.draw(Primitive.TRIANGLE, offsetAny / vertexLayout.bytesPerVertex, sizeAny);
			return sizeAny;
		}
		else if(sizeAny == 0) {
			renderingContext.draw(Primitive.TRIANGLE, offsetAlso / vertexLayout.bytesPerVertex, sizeAlso);
			return sizeAlso;
		}
		else {
			array[0] = offsetAny / vertexLayout.bytesPerVertex;
			array[2] = offsetAlso / vertexLayout.bytesPerVertex;
			array[1] = sizeAny;
			array[3] = sizeAlso;
			renderingContext.drawMany(Primitive.TRIANGLE, array);
			return sizeAny + sizeAlso;
		}
		
		//throw new RuntimeException("Unsupported vertex layout in "+this);
	}
	
	public enum RenderLodLevel {
		HIGH,
		LOW
	}
}
