package io.xol.chunkstories.api.world.heightmap;

import java.util.Iterator;

import io.xol.chunkstories.api.util.concurrency.Fence;
import io.xol.chunkstories.api.world.chunk.WorldUser;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface RegionSummary
{
	public boolean isLoaded();
	
	public Fence waitForLoading();
	
	public boolean registerUser(WorldUser user);

	public boolean unregisterUser(WorldUser user);

	public Iterator<WorldUser> getSummaryUsers();

	public void updateOnBlockModification(int worldX, int height, int worldZ, int voxelData);
	
	public void setHeightAndId(int worldX, int height, int worldZ, int voxelData);

	public int getHeight(int x, int z);

	public int getVoxelData(int x, int z);

	public int getRegionX();
	
	public int getRegionZ();
}
