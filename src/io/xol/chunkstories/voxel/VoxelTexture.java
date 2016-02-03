package io.xol.chunkstories.voxel;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class VoxelTexture
{
	public VoxelTexture(String name) throws Exception
	{
		this.name = name;
		if(name == null)
			throw new Exception("fuck off m9");
	}

	public String name;
	public int legacyId;

	public int imageFileDimensions;
	public int textureScale = 1;

	public int atlasOffset;
	public int atlasT;
	public int atlasS;
}
