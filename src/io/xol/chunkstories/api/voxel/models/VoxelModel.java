package io.xol.chunkstories.api.voxel.models;

import io.xol.chunkstories.api.Content;

public interface VoxelModel extends VoxelRenderer
{
	public String getName();

	public int getSizeInVertices();

	public boolean[][] getCulling();

	public String[] getTexturesNames();

	public int[] getTexturesOffsets();

	public float[] getVertices();

	public float[] getTexCoords();

	public float[] getNormals();

	public byte[] getExtra();

	public float getJitterX();

	public float getJitterY();

	public float getJitterZ();

	public Content.Voxels.VoxelModels store();

}