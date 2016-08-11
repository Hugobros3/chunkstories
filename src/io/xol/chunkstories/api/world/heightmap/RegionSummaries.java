package io.xol.chunkstories.api.world.heightmap;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.world.chunk.WorldUser;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface RegionSummaries
{
	public RegionSummary aquireRegionSummary(WorldUser worldUser, int regionX, int regionZ);
	
	public RegionSummary aquireRegionSummaryChunkCoordinates(WorldUser worldUser, int chunkX, int chunkZ);
	
	public RegionSummary aquireRegionSummaryWorldCoordinates(WorldUser worldUser, int worldX, int worldZ);
	
	public RegionSummary aquireRegionSummaryLocation(WorldUser worldUser, Location location);
	
	public int getHeightAtWorldCoordinates(int worldX, int worldZ);
	public int getDataAtWorldCoordinates(int worldX, int worldZ);
}
