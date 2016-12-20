package io.xol.chunkstories.voxel;

import io.xol.engine.math.lalgb.vector.sp.Vector4fm;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class VoxelTexture
{
	public VoxelTexture(String name, int id) throws Exception
	{
		this.name = name;
		this.id = id;
		if(name == null)
			throw new Exception("fuck off m9");
	}

	public String name;
	public int id;

	public int imageFileDimensions;
	public int textureScale = 1;

	public int atlasOffset;
	public int atlasT;
	public int atlasS;
	
	public Vector4fm color;
	public int positionInColorIndex;
}
