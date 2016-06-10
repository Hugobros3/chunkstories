package io.xol.chunkstories.api.world;

import java.util.Iterator;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.world.chunk.CubicChunk;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface WorldInterface
{
	/**
	 * Adds an entity to the world, the entity location is supposed to be already defined
	 * @param entity
	 */
	void addEntity(Entity entity);

	/**
	 * Removes an entity from the world, based on UUID
	 * @param entity
	 */
	void removeEntity(Entity entity);

	/**
	 * Game-logic function. Not something you'd be supposed to call
	 */
	void tick();

	/**
	 * Returns an iterator containing all the loaded entities.
	 * Supposedly thread-safe
	 * @return
	 */
	Iterator<Entity> getAllLoadedEntities();

	/**
	 * @param entityID a valid UUID
	 * @return null if it can't be found
	 */
	Entity getEntityByUUID(long entityID);

	/**
	 * As of the current version of the game, this is internally set to 1024
	 * @return
	 */
	int getMaxHeight();

	/**
	 * Return the world size devided by 32.
	 * @return
	 */
	int getSizeInChunks();

	/**
	 * Return the world size (length of each square side)
	 * @return
	 */
	double getWorldSize();

	/**
	 * Returns the block data at the specified location
	 * Will try to load/generate the chunks if not alreay in ram
	 * @param location
	 * @return The raw block data, see {@link VoxelFormat}
	 */
	int getDataAt(Location location);

	/**
	 * Returns the block data at the specified location
	 * @param location
	 * @param load If set to false, will *not* try to load the chunk if it's not present and will instead return 0
	 * @return The raw block data, see {@link VoxelFormat}
	 */
	int getDataAt(Location location, boolean load);

	/**
	 * Returns the block data at the specified location
	 * Will try to load/generate the chunks if not alreay in ram
	 * @param x
	 * @param y
	 * @param z
	 * @return The raw block data, see {@link VoxelFormat}
	 */
	int getDataAt(int x, int y, int z);

	/**
	 * Returns the block data at the specified location
	 * @param x
	 * @param y
	 * @param z
	 * @param load If set to false, will *not* try to load the chunk if it's not present and will instead return 0
	 * @return The raw block data, see {@link VoxelFormat}
	 */
	int getDataAt(int x, int y, int z, boolean load);

	/**
	 * Sets the block data at the specified location
	 * Will try to load/generate the chunks if not alreay in ram
	 * @param x
	 * @param y
	 * @param z
	 * @param i The new data to set the block to, see {@link VoxelFormat}
	 */
	void setDataAt(int x, int y, int z, int i);

	/**
	 * Sets the block data at the specified location
	 * Will try to load/generate the chunks if not alreay in ram
	 * @param location
	 * @param i The new data to set the block to, see {@link VoxelFormat}
	 */
	void setDataAt(Location location, int i);

	/**
	 * Sets the block data at the specified location
	 * @param location
	 * @param i The new data to set the block to, see {@link VoxelFormat}
	 * @param load If set to false, will *not* try to load the chunk if it's not present
	 */
	void setDataAt(Location location, int i, boolean load);

	/**
	 * Sets the block data at the specified location
	 * @param x
	 * @param y
	 * @param z
	 * @param i The new data to set the block to, see {@link VoxelFormat}
	 * @param load If set to false, will *not* try to load the chunk if it's not present
	 */
	void setDataAt(int x, int y, int z, int i, boolean load);

	/**
	 * @param x
	 * @param y
	 * @param z
	 * @return The sun light level of the block per {@link VoxelFormat} ( 0-15 ) using either getDataAt if the chunk is loaded or
	 * the heightmap ( y <= heightmapLevel(x, z) ? 0 : 15 )
	 */
	int getSunlightLevel(int x, int y, int z);
	
	int getSunlightLevel(Location location);
	
	/**
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @return Returns the block light level of the block per {@link VoxelFormat} ( 0-15 ) using getDataAt ( if the chunk isn't loaded it will return a zero. )
	 */
	int getBlocklightLevel(int x, int y, int z);

	int getBlocklightLevel(Location location);
	
	/**
	 * Returns null or a chunk. If the load flag is set to true, it will also try to load it ingame
	 * @param chunkX
	 * @param chunkY
	 * @param chunkZ
	 * @param load
	 * @return
	 */
	CubicChunk getChunk(int chunkX, int chunkY, int chunkZ, boolean load);

	ChunksIterator getAllLoadedChunks();
	
	/**
	 * Unloads forcefully a chunk
	 * @param c
	 * @param save
	 */
	void removeChunk(CubicChunk c, boolean save);

	void removeChunk(int chunkX, int chunkY, int chunkZ, boolean save);

	/**
	 * Unloads bits of the map not required by anyone
	 */
	void trimRemovableChunks();
	/**
	 * @param chunkX
	 * @param chunkY
	 * @param chunkZ
	 * @return True if the chunk is loaded
	 */
	boolean isChunkLoaded(int chunkX, int chunkY, int chunkZ);
	
	/**
	 * Loads or replaces an entire chunk with another
	 * @param chunk
	 */
	void setChunk(CubicChunk chunk);

	/**
	 * For dirty hacks that need so
	 */
	void redrawAllChunks();

	/**
	 * Unloads everything
	 */
	void unloadEverything();

	/**
	 * Blocking method saving all loaded chunks
	 */
	void saveEverything();

	/**
	 * Destroys the world, kill threads and frees stuff
	 */
	void destroy();

	boolean isRaining();

	void setWeather(boolean isRaining);

	Location getDefaultSpawnLocation();

	/**
	 * Sets the time of the World. By default the time is set at 5000 and it uses a 10.000 cycle, 0 being midnight and 5000 being midday
	 * @param time
	 */
	void setTime(long time);

	WorldGenerator getGenerator();

	/**
	 * Called when some controllable entity try to interact with the world
	 * @param entity
	 * @param blockLocation
	 * @param input
	 * @return
	 */
	boolean handleInteraction(Entity entity, Location blockLocation, Input input);

}