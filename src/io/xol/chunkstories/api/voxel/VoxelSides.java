package io.xol.chunkstories.api.voxel;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public enum VoxelSides
{
	/**	 Conventions for space in Chunk Stories
	 
	           1    FRONT z+
	 x- LEFT 0 X 2  RIGHT x+
	           3    BACK  z-
	 4 y+ top
	 X
	 5 y- bottom
	 */
	
	LEFT,
	FRONT,
	RIGHT,
	BACK,
	TOP,
	BOTTOM;
}
