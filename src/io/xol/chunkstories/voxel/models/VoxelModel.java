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
	
	public float[] vertices;
	public float[] texCoords;
	public float[] normals;

	public float jitterX = 0;
	public float jitterY = 0;
	public float jitterZ = 0;
}
