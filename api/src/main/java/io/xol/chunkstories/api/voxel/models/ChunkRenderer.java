package io.xol.chunkstories.api.voxel.models;

import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.LodLevel;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.ShadingType;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** This interface is the context in which chunks are rendered, it's implemented by the thread-pool rendering the chunks */
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
		
		/** Returns the INTERNAL X coordinate of the voxel currently being rendered, in the chunk (0-31) */
		public int getRenderedVoxelPositionInChunkX();
		
		/** Returns the INTERNAL Y coordinate of the voxel currently being rendered, in the chunk (0-31) */
		public int getRenderedVoxelPositionInChunkY();
		
		/** Returns the INTERNAL Z coordinate of the voxel currently being rendered, in the chunk (0-31) */
		public int getRenderedVoxelPositionInChunkZ();
		
		public VoxelLighter getCurrentVoxelLighter();
		
		public interface VoxelLighter {
			/** Returns a value between 0 and 15 if the block is non-opaque, -1 if it is */
			public byte getSunlightLevelForCorner(VoxelSides.Corners corner);

			/** Returns a value between 0 and 15 if the block is non-opaque, -1 if it is */
			public byte getBlocklightLevelForCorner(VoxelSides.Corners corner);
			
			public byte getAoLevelForCorner(VoxelSides.Corners corner);

			public byte getSunlightLevelInterpolated(float vertX, float vertY, float vertZ);
			public byte getBlocklightLevelInterpolated(float vertX, float vertY, float vertZ);
			public byte getAoLevelInterpolated(float vertX, float vertY, float vertZ);
		}
	}
}
