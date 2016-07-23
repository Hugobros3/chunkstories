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

	//Vanilla mc sides (stairs) 
	// 1 = cs_RIGHT / mc_WEST   |    3
	// 0 = cs_LEFT  / mc_EAST   |  0 X 1
	// 2 = cs_BACK  / mc_SOUTH  |    2
	// 3 = cs_FRONT / mc_NORTH  |
	
	LEFT,
	FRONT,
	RIGHT,
	BACK,
	TOP,
	BOTTOM;
	
	/**
	 * Returns the Chunk Stories side from the minecraft metadata of the following objects, no top/bottom direction allowed
	 */
	public static VoxelSides getSideMcStairsChestFurnace(int mcSide)
	{
		switch(mcSide)
		{
		case 2:
			return FRONT;
		case 3:
			return BACK;
		case 4:
			return RIGHT;
		case 5:
			return LEFT;
		}
		
		return FRONT;
	}
}
