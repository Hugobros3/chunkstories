package io.xol.chunkstories.api.world.heightmap;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.world.chunk.WorldUser;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface RegionSummaries
{
	/**
	 * Aquires a region summary and registers an user, triggering a load operation for the region summary and preventing it to unload until all the users
	 *  either unregisters or gets garbage collected and their references nulls out.
	 */
	public RegionSummary aquireRegionSummary(WorldUser worldUser, int regionX, int regionZ);
	
	/**
	 * Aquires a region summary and registers an user, triggering a load operation for the region summary and preventing it to unload until all the users
	 *  either unregisters or gets garbage collected and their references nulls out.
	 */
	public RegionSummary aquireRegionSummaryChunkCoordinates(WorldUser worldUser, int chunkX, int chunkZ);
	
	/**
	 * Aquires a region summary and registers an user, triggering a load operation for the region summary and preventing it to unload until all the users
	 *  either unregisters or gets garbage collected and their references nulls out.
	 */
	public RegionSummary aquireRegionSummaryWorldCoordinates(WorldUser worldUser, int worldX, int worldZ);
	
	/**
	 * Aquires a region summary and registers an user, triggering a load operation for the region summary and preventing it to unload until all the users
	 *  either unregisters or gets garbage collected and their references nulls out.
	 */
	public RegionSummary aquireRegionSummaryLocation(WorldUser worldUser, Location location);
	
	/**
	 * Returns either null or a valid, entirely loaded region summary if the aquireRegionSummary method was called and it had time to load and there is still one user using it
	 */
	public RegionSummary getRegionSummary(int regionX, int regionZ);
	
	/**
	 * Returns either null or a valid, entirely loaded region summary if the aquireRegionSummary method was called and it had time to load and there is still one user using it
	 */
	public RegionSummary getRegionSummaryChunkCoordinates(int chunkX, int chunkZ);
	
	/**
	 * Returns either null or a valid, entirely loaded region summary if the aquireRegionSummary method was called and it had time to load and there is still one user using it
	 */
	public RegionSummary getRegionSummaryWorldCoordinates(int worldX, int worldZ);
	
	/**
	 * Returns either null or a valid, entirely loaded region summary if the aquireRegionSummary method was called and it had time to load and there is still one user using it
	 */
	public RegionSummary getRegionSummaryLocation(Location location);
	
	public int getHeightAtWorldCoordinates(int worldX, int worldZ);
	
	public int getDataAtWorldCoordinates(int worldX, int worldZ);
}
