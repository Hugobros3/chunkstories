//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.renderer.chunks;

import java.util.ArrayList;
import java.util.List;

import io.xol.chunkstories.api.rendering.vertex.RecyclableByteBuffer;
import io.xol.chunkstories.api.rendering.voxel.VoxelDynamicRenderer;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.engine.concurrency.SimpleFence;

public class ChunkMeshDataSections
{
	public final int[][][] vertices_type_size;
	public final int[][][] vertices_type_offset;
	
	RecyclableByteBuffer dataToUpload;
	SimpleFence fence = new SimpleFence();
	
	DynamicallyRenderedVoxelType[] dynamicVoxelTypes = null;
	
	/** Represents a Voxel that exposes a dynamic renderer and the indexes of the cells within a chunk that correspond to it */
	public static class DynamicallyRenderedVoxelType {
		final Voxel voxelType;
		final VoxelDynamicRenderer renderer;
		final List<Integer> indexes = new ArrayList<>(); //TODO just use a regular array for dem speed
		
		public DynamicallyRenderedVoxelType(VoxelDynamicRenderer dynamicRenderer, Voxel voxelType) {
			this.renderer = dynamicRenderer;
			this.voxelType = voxelType;
		}
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
