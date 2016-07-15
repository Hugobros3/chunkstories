package io.xol.chunkstories.api.world;

import java.util.Iterator;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.particles.ParticleData;
import io.xol.chunkstories.api.particles.ParticleType;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.world.heightmap.RegionSummaries;

import io.xol.chunkstories.world.WorldInfo;
import io.xol.engine.math.lalgb.Vector3d;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface World
{
	/*
	 * Entities management
	 */
	
	/**
	 * Adds an entity to the world, the entity location is supposed to be already defined
	 * @param entity
	 */
	public void addEntity(Entity entity);

	/**
	 * Removes an entity from the world, matches the object
	 * @param entity
	 */
	public boolean removeEntity(Entity entity);
	
	/**
	 * Removes an entity from the world, based on UUID
	 * @param entity
	 */
	public boolean removeEntity(long uuid);

	/**
	 * Returns an iterator containing all the loaded entities.
	 * Supposedly thread-safe
	 * @return
	 */
	public Iterator<Entity> getAllLoadedEntities();

	/**
	 * @param entityID a valid UUID
	 * @return null if it can't be found
	 */
	public Entity getEntityByUUID(long uuid);

	/*
	 * World parameters
	 */
	
	/**
	 * As of the current version of the game, this is internally set to 1024
	 * @return
	 */
	public int getMaxHeight();

	/**
	 * Return the world size devided by 32.
	 * @return
	 */
	public int getSizeInChunks();

	/**
	 * Return the world size (length of each square side)
	 * @return
	 */
	public double getWorldSize();

	/*
	 * Get data
	 */
	
	/**
	 * Returns the block data at the specified location
	 * Will try to load/generate the chunks if not alreay in ram
	 * @return The raw block data, see {@link VoxelFormat}
	 */
	public int getVoxelData(Location location);

	/**
	 * Returns the block data at the specified location
	 * @param loadIfNotPresent If set to false, will *not* try to load the chunk if it's not present and will instead return 0
	 * @return The raw block data, see {@link VoxelFormat}
	 */
	public int getVoxelData(Location location, boolean loadIfNotPresent);

	/**
	 * Returns the block data at the specified location
	 * Will try to load/generate the chunks if not alreay in ram
	 * @return The raw block data, see {@link VoxelFormat}
	 */
	public int getVoxelData(int x, int y, int z);

	/**
	 * Returns the block data at the specified location
	 * @param loadIfNotPresent If set to false, will *not* try to load the chunk if it's not present and will instead return 0
	 * @return The raw block data, see {@link VoxelFormat}
	 */
	public int getVoxelData(int x, int y, int z, boolean loadIfNotPresent);

	/*
	 * Set data
	 */
	
	/**
	 * Sets the block data at the specified location
	 * Will try to load/generate the chunks if not alreay in ram
	 * @param data The new data to set the block to, see {@link VoxelFormat}
	 */
	public void setVoxelData(int x, int y, int z, int data);

	/**
	 * Sets the block data at the specified location
	 * Will try to load/generate the chunks if not alreay in ram
	 * @param data The new data to set the block to, see {@link VoxelFormat}
	 */
	public void setVoxelData(Location location, int data);

	/**
	 * Sets the block data at the specified location
	 * @param data The new data to set the block to, see {@link VoxelFormat}
	 * @param load If set to false, will *not* try to load the chunk if it's not present
	 */
	public void setVoxelData(Location location, int data, boolean load);

	/**
	 * Sets the block data at the specified location
	 * @param data The new data to set the block to, see {@link VoxelFormat}
	 * @param load If set to false, will *not* try to load the chunk if it's not present
	 */
	public void setVoxelData(int x, int y, int z, int data, boolean load);
	
	/**
	 * Method to call when it's an entity that do the action to set the voxel data
	 * @param data The new data to set the block to, see {@link VoxelFormat}
	 */
	public void setVoxelData(Location location, int data, Entity entity);

	/**
	 * Method to call when it's an entity that do the action to set the voxel data
	 * @param data The new data to set the block to, see {@link VoxelFormat}
	 */
	public void setVoxelData(int x, int y, int z, int data, Entity entity);

	/**
	 * Only sets the data, don't trigger any logic, rendering etc
	 * @param data
	 * @param load
	 */
	public void setVoxelDataWithoutUpdates(int x, int y, int z, int data, boolean load);
	
	/*
	 * Voxel light
	 */
	
	/**
	 * @return The sun light level of the block per {@link VoxelFormat} ( 0-15 ) using either getDataAt if the chunk is loaded or
	 * the heightmap ( y <= heightmapLevel(x, z) ? 0 : 15 )
	 */
	public int getSunlightLevel(int x, int y, int z);
	
	public int getSunlightLevel(Location location);
	
	/**
	 * @return Returns the block light level of the block per {@link VoxelFormat} ( 0-15 ) using getDataAt ( if the chunk isn't loaded it will return a zero. )
	 */
	public int getBlocklightLevel(int x, int y, int z);

	public int getBlocklightLevel(Location location);

	/*
	 * Chunks
	 */
	
	public ChunksIterator getAllLoadedChunks();
	
	/**
	 * Unloads forcefully a chunk
	 * @param c
	 * @param save
	 */
	public void removeChunk(Chunk c, boolean save);

	public void removeChunk(int chunkX, int chunkY, int chunkZ, boolean save);

	/**
	 * Unloads bits of the map not required by anyone
	 */
	public void trimRemovableChunks();
	/**
	 * @param chunkX
	 * @param chunkY
	 * @param chunkZ
	 * @return True if the chunk is loaded
	 */
	public boolean isChunkLoaded(int chunkX, int chunkY, int chunkZ);
	
	/**
	 * Loads or replaces an entire chunk with another
	 * @param chunk
	 */
	public void setChunk(Chunk chunk);
	
	/**
	 * Returns null or a chunk. If the load flag is set to true, it will also try to load it ingame
	 * @param load
	 * @return
	 */
	public Chunk getChunk(int chunkX, int chunkY, int chunkZ, boolean load);
	
	public Region getRegionWorldCoordinates(int worldX, int worldY, int worldZ);

	public Region getRegionChunkCoordinates(int chunkX, int chunkY, int chunkZ);
	
	public Region getRegion(int regionX, int regionY, int regionZ);
	
	/*
	 * Global methods
	 */
	
	/**
	 * For dirty hacks that need so
	 */
	public void redrawEverything();

	/**
	 * Unloads everything
	 */
	public void unloadEverything();

	/**
	 * Blocking method saving all loaded chunks
	 */
	public void saveEverything();

	/**
	 * Destroys the world, kill threads and frees stuff
	 */
	public void destroy();

	/**
	 * The weather is represented by a normalised float value
	 * 0.0 equals dead dry
	 * 0.2 equals sunny
	 * 0.4 equals overcast
	 * 0.5 equals foggy/cloudy
	 * >0.5 rains
	 * 0.8 max rain intensity
	 * 0.9 lightning
	 * 1.0 hurricane
	 * @return
	 */
	public float getWeather();

	public void setWeather(float overcastFactor);

	public Location getDefaultSpawnLocation();

	/**
	 * Sets the time of the World. By default the time is set at 5000 and it uses a 10.000 cycle, 0 being midnight and 5000 being midday
	 * @param time
	 */
	public void setTime(long time);

	/**
	 * Game-logic function. Not something you'd be supposed to call
	 */
	public void tick();

	public WorldGenerator getGenerator();

	/**
	 * Called when some controllable entity try to interact with the world
	 * @return true if the interaction was handled
	 */
	public boolean handleInteraction(Entity entity, Location blockLocation, Input input);
	
	/*
	 * Raytracers and methods to grab entities
	 */
	
	/**
	 * Raytraces throught the world to find a solid block
	 * @param limit Between 0 and a finite number
	 * @return The exact location of the intersection or null if it didn't found one
	 */
	public Location raytraceSolid(Vector3d initialPosition, Vector3d direction, double limit);
	
	/**
	 * Raytraces throught the world to find a solid block
	 * @param limit Between 0 and a finite number
	 * @return The exact location of the step just before the intersection ( as to get the adjacent block ) or null if it didn't found one
	 */
	public Location raytraceSolidOuter(Vector3d initialPosition, Vector3d direction, double limit);
	
	/**
	 * Raytraces throught the world to find a solid or selectable block
	 * @param limit Between 0 and a finite number
	 * @return The exact location of the intersection or null if it didn't found one
	 */
	public Location raytraceSelectable(Location initialPosition, Vector3d direction, double limit);
	
	/**
	 * Takes into account the voxel terrain and will stop at a solid block, <b>warning</b> limit can't be == -1 !
	 * @param limit Between 0 and a finite number
	 * @return Returns all entities that intersects with the ray within the limit, ordered nearest to furthest
	 */
	public Iterator<Entity> rayTraceEntities(Vector3d initialPosition, Vector3d direction, double limit);

	/**
	 * Ignores any terrain
	 * @param limit Either -1 or between 0 and a finite number
	 * @return Returns all entities that intersects with the ray within the limit, ordered nearest to furthest
	 */
	public Iterator<Entity> raytraceEntitiesIgnoringVoxels(Vector3d initialPosition, Vector3d direction, double limit);
	
	/*
	 * Fx
	 */

	public ParticleData addParticle(ParticleType particleType, Vector3d eyeLocation);
	
	public ParticleData addParticle(ParticleType particleType, ParticleData particleData);

	public void playSoundEffect(String soundEffect, Location location, float pitch, float gain);

	public RegionSummaries getRegionSummaries();

	public WorldInfo getWorldInfo();

	public long getTime();
}