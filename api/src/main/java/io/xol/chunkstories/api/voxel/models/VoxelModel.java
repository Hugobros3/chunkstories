package io.xol.chunkstories.api.voxel.models;

import io.xol.chunkstories.api.Content;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** Represents a voxel .model file loaded by the engine. Look up the syntax on the wiki for more information. */
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