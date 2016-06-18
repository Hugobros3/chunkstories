package io.xol.chunkstories.api.world;

import java.util.Iterator;

import io.xol.chunkstories.api.entity.Entity;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface Region
{
	public int getRegionX();
	
	public int getRegionY();
	
	public int getRegionZ();

	/**
	 * @return An iterator over each entity within this region
	 */
	Iterator<Entity> getEntitiesWithinRegion();
	
	/**
	 * Called when the entity is no longer within this region's influence
	 * @return If it was removed successfully
	 */
	public boolean removeEntity(Entity entity);
	
	/**
	 * Called when the entity is now within this region's influence
	 * @return If it was added successfully
	 */
	public boolean addEntity(Entity entity);
	
	int getNumberOfLoadedChunks();

	boolean isLoaded();
	
	Chunk get(int chunkX, int chunkY, int chunkZ, boolean load);

	boolean isChunkLoaded(int chunkX, int chunkY, int chunkZ);

	boolean removeChunk(int chunkX, int chunkY, int chunkZ);

	void save();

}