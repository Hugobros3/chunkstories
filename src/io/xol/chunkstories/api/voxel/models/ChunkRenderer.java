package io.xol.chunkstories.api.voxel.models;

import io.xol.chunkstories.api.voxel.VoxelSides;
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
		
		/** Returns the INTERNAL coordinate of the voxel currently being rendered, in the chunk */
		public int getRenderedVoxelPositionInChunkX();
		public int getRenderedVoxelPositionInChunkY();
		public int getRenderedVoxelPositionInChunkZ();
		
		public VoxelLighter getCurrentVoxelLighter();
		
		public interface VoxelLighter {
			/** Returns a value between 0 and 15 if the block is non-opaque, -1 if it is */
			public byte getSunlightLevelForCorner(VoxelSides.Corners corner);

			/** Returns a value between 0 and 15 if the block is non-opaque, -1 if it is */
			public byte getBlocklightLevelForCorner(VoxelSides.Corners corner);
			
			public byte getAoLevelForCorner(VoxelSides.Corners corner);
		}
	}
}
