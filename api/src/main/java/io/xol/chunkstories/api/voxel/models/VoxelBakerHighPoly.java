package io.xol.chunkstories.api.voxel.models;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** Used to create intricate models, with floating point coordinates. Consumes more VRAM. */
public interface VoxelBakerHighPoly extends VoxelBakerCommon
{
	public void addVerticeFloat(float f0, float f1, float f2);
}
