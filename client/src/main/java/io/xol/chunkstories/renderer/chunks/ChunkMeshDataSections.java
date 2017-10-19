package io.xol.chunkstories.renderer.chunks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.xol.chunkstories.api.rendering.vertex.RecyclableByteBuffer;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.components.VoxelComponentDynamicRenderer.VoxelDynamicRenderer;
import io.xol.engine.concurrency.SimpleFence;

public class ChunkMeshDataSections
{
	public final int[][][] vertices_type_size;
	public final int[][][] vertices_type_offset;
	
	RecyclableByteBuffer dataToUpload;
	SimpleFence fence = new SimpleFence();
	
	Map<Voxel, DynamicallyRenderedVoxelClass> dynamicallyRenderedVoxels;
	
	public static class DynamicallyRenderedVoxelClass {
		VoxelDynamicRenderer renderer;
		List<Integer> indexes = new ArrayList<>();
	}
	
	public ChunkMeshDataSections(RecyclableByteBuffer dataToUpload, int[][][] vertices_type_size, int[][][] vertices_type_offset)
	{
		this.dataToUpload = dataToUpload;
		this.vertices_type_size = vertices_type_size;
		this.vertices_type_offset = vertices_type_offset;
	}

	public void notNeeded() {
		
		//Your life was a lie
		dataToUpload.recycle();
		
		fence.signal();
	}
	
	public void consumed() {
		fence.signal();
	}
}
