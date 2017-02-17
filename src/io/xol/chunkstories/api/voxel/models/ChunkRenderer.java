package io.xol.chunkstories.api.voxel.models;

import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.LodLevel;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.RenderPass;

public interface ChunkRenderer
{
	public VoxelBakerHighPoly getHighpolyBakerFor(LodLevel lodLevel, RenderPass renderPass);
	
	public VoxelBakerCubic getLowpolyBakerFor(LodLevel lodLevel, RenderPass renderPass);
	
	public interface ChunkRenderContext {
		public boolean isTopChunkLoaded();
		public boolean isBottomChunkLoaded();
		public boolean isLeftChunkLoaded();
		public boolean isRightChunkLoaded();
		public boolean isFrontChunkLoaded();
		public boolean isBackChunkLoaded();
	}
}
