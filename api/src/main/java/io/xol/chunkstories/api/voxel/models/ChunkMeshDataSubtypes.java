package io.xol.chunkstories.api.voxel.models;

/** Chunk meshes are made of sections containing various data types */
public final class ChunkMeshDataSubtypes {

	/** Cf VoxelBakerLowLod and VoxelBakerHighLod */
	public enum VertexLayout {
		WHOLE_BLOCKS(16),
		INTRICATE(24);
		
		public final int bytesPerVertex;

		VertexLayout(int bytesPerVertex) {
			this.bytesPerVertex = bytesPerVertex;
		}
	}
	
	public enum LodLevel {
		ANY, //Will be rendered no matter what
		LOW,  //Rendered only when chunk is far enough
		HIGH  //Rendered when LOW isn't
	}
	
	public enum ShadingType {
		OPAQUE,
		LIQUIDS,
		SEMI_TRANSPARENT,
		CUSTOM0,
		CUSTOM1,
		CUSTOM2,
		CUSTOM3,
		;
	}
}