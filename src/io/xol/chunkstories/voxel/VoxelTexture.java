package io.xol.chunkstories.voxel;

import java.io.File;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class VoxelTexture
{

	public VoxelTexture(File file, String name)
	{
		this.file = file;
		this.name = name;
	}

	public File file;
	public String name;
	public int legacyId;

	public int imageFileDimensions;
	public int textureScale = 1;

	public int atlasOffset;
	public int atlasT;
	public int atlasS;
}
