package io.xol.chunkstories.api.world.heightmap;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface RegionSummaries
{
	public int getHeightAtWorldCoordinates(int worldX, int worldZ);
	public int getDataAtWorldCoordinates(int worldX, int worldZ);
}
