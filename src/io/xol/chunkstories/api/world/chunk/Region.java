package io.xol.chunkstories.api.world.chunk;

import java.util.Iterator;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityVoxel;
import io.xol.chunkstories.api.world.World;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * A region is 8x8x8 chunks and contains entities
 */
public interface Region
{
	public int getRegionX();
	
	public int getRegionY();
	
	public int getRegionZ();

	Iterator<WorldUser> getChunkUsers();

	boolean registerUser(WorldUser user);

	boolean unregisterUser(WorldUser user);
	
	/**
	 * Called when the entity is now within this region's influence
	 * @return If it was added successfully
	 */
	public boolean addEntityToRegion(Entity entity);
	
	/**
	 * Called when the entity is no longer within this region's influence
	 * @return If it was removed successfully
	 */
	public boolean removeEntityFromRegion(Entity entity);
	
	/**
	 * @return An iterator over each entity within this region
	 */
	Iterator<Entity> getEntitiesWithinRegion();
	
	public int getNumberOfLoadedChunks();

	public boolean isDiskDataLoaded();
	
	public ChunkHolder getChunkHolder(int chunkX, int chunkY, int chunkZ);
	
	public Chunk getChunk(int chunkX, int chunkY, int chunkZ);

	public boolean isChunkLoaded(int chunkX, int chunkY, int chunkZ);

	public boolean isUnused();

	//public boolean removeChunk(int chunkX, int chunkY, int chunkZ);

	public void save();

	public void unloadAndSave();

	public World getWorld();

	public EntityVoxel getEntityVoxelAt(int worldX, int worldY, int worldZ);
}