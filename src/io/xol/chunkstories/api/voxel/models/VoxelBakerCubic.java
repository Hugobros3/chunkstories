package io.xol.chunkstories.api.voxel.models;

import io.xol.chunkstories.api.voxel.VoxelSides.Corners;
import io.xol.chunkstories.api.voxel.models.ChunkRenderer.ChunkRenderContext.VoxelLighter;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** Only able to have whole/integer vertice coordinates, saves VRAM. */
public interface VoxelBakerCubic extends VoxelBakerCommon
{
	public void addVerticeInt(int i0, int i1, int i2);

	public void addColorsAuto(VoxelLighter voxelLighter, Corners corner);
}
