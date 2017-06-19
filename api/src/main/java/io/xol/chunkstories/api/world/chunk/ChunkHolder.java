package io.xol.chunkstories.api.world.chunk;

import java.util.Iterator;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Regions have 8x8x8 slots for chunks, this holds the offline/compressed data and takes care of loading/unloading the live data when required by users
 */
public interface ChunkHolder
{
	/**
	 * @return The region this slot is in
	 */
	public Region getRegion();

	/**
	 * @return The X coordinate of the chunk slot within the region [0 : 8]
	 */
	public int getInRegionX();
	
	/**
	 * @return The Y coordinate of the chunk slot within the region [0 : 8]
	 */
	public int getInRegionY();
	
	/**
	 * @return The Z coordinate of the chunk slot within the region [0 : 8]
	 */
	public int getInRegionZ();
	
	public int getChunkCoordinateX();
	public int getChunkCoordinateY();
	public int getChunkCoordinateZ();
	
	public Iterator<WorldUser> getChunkUsers();
	
	/**
	 * @return True if the user has successfully been added to the holder, false if it was already inside
	 */
	public boolean registerUser(WorldUser user);
	
	/**
	 * @return True uppon successfull removal of the user
	 */
	public boolean unregisterUser(WorldUser user);
	
	public Chunk getChunk();
	
	public default boolean isChunkLoaded()
	{
		return getChunk() != null;
	}
	
	public void compressChunkData();
}
