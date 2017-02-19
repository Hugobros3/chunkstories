package io.xol.chunkstories.api.voxel.models;

import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.LodLevel;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.ShadingType;

public interface ChunkRenderer
{
	public VoxelBakerHighPoly getHighpolyBakerFor(LodLevel lodLevel, ShadingType renderPass);
	
	public VoxelBakerCubic getLowpolyBakerFor(LodLevel lodLevel, ShadingType renderPass);
	
	public interface ChunkRenderContext {
		public boolean isTopChunkLoaded();
		public boolean isBottomChunkLoaded();
		public boolean isLeftChunkLoaded();
		public boolean isRightChunkLoaded();
		public boolean isFrontChunkLoaded();
		public boolean isBackChunkLoaded();
	}
}
