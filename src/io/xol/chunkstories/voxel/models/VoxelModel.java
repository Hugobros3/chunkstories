package io.xol.chunkstories.voxel.models;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class VoxelModel
{
	public String name;

	public VoxelModel(String name)
	{
		this.name = name;
	}
	
	public boolean[][] culling;
	
	public float[][] vertices;
	public float[][] texCoords;
	public float[][] normals;
}
