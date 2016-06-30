package io.xol.chunkstories.api.world;

import java.util.Iterator;

import io.xol.chunkstories.api.entity.Entity;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * A region holds 8x8x8 chunks, it's used to hold their entities
 */
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
	
	public int getNumberOfLoadedChunks();

	public boolean isDiskDataLoaded();
	
	public Chunk getChunk(int chunkX, int chunkY, int chunkZ, boolean load);

	public boolean isChunkLoaded(int chunkX, int chunkY, int chunkZ);

	public boolean removeChunk(int chunkX, int chunkY, int chunkZ);

	public void save();

	public void unloadAndSave();

	public World getWorld();

}