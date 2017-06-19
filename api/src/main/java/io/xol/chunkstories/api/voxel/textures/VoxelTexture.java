package io.xol.chunkstories.api.voxel.textures;

import io.xol.chunkstories.api.math.vector.sp.Vector4fm;

public interface VoxelTexture
{

	Vector4fm getColor();

	int getAtlasS();

	int getAtlasT();

	int getAtlasOffset();

	int getTextureScale();

	String getName();

}
