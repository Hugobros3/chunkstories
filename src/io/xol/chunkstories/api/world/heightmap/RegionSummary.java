package io.xol.chunkstories.api.world.heightmap;

import java.util.Iterator;

import io.xol.chunkstories.api.world.chunk.WorldUser;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface RegionSummary
{
	boolean registerUser(WorldUser user);

	boolean unregisterUser(WorldUser user);

	Iterator<WorldUser> getSummaryUsers();

	void updateOnBlockModification(int worldX, int height, int worldZ, int voxelData);
	void setHeightAndId(int worldX, int height, int worldZ, int voxelData);

	int getHeight(int x, int z);

	int getVoxelData(int x, int z);

	int getRegionX();
	int getRegionZ();
}
