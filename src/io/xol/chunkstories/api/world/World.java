package io.xol.chunkstories.api.world;

import java.util.Iterator;

import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.GameLogic;
import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.particles.ParticlesManager;
import io.xol.chunkstories.api.rendering.effects.DecalsManager;
import io.xol.chunkstories.api.sound.SoundManager;
import io.xol.chunkstories.api.utils.IterableIterator;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.api.world.chunk.ChunkHolder;
import io.xol.chunkstories.api.world.chunk.WorldUser;
import io.xol.chunkstories.api.world.chunk.ChunksIterator;
import io.xol.chunkstories.api.world.chunk.Region;
import io.xol.chunkstories.api.world.heightmap.RegionSummaries;

import io.xol.chunkstories.world.WorldInfo;
import io.xol.engine.math.lalgb.vector.dp.Vector3dm;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface World
{
	public WorldInfo getWorldInfo();
	
	public WorldGenerator getGenerator();
	
	/** Returns the GameLogic thread this world runs on */
	public GameLogic getGameLogic();
	
	/** Returns the GameContext this world lives in */
	public GameContext getGameContext();
	
	/**
	 * @return The height of the world, default worlds are 1024
	 */
	public default int getMaxHeight()
	{
		return getWorldInfo().getSize().heightInChunks * 32;
	}

	/**
	 * @return The size of the side of the world, divided by 32
	 */
	public default int getSizeInChunks()
	{
		return getWorldInfo().getSize().sizeInChunks;
	}

	/**
	 * @return The size of the side of the world
	 */
	public default double getWorldSize()
	{
		return getSizeInChunks() * 32;
	}
	
	/* Entity management */
	
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
	 * @param entityFollowed
	 */
	public boolean removeEntityByUUID(long uuid);

	/**
	 * @param entityID a valid UUID
	 * @return null if it can't be found
	 */
	public Entity getEntityByUUID(long uuid);

	/**
	 * Returns an iterator containing all the loaded entities.
	 * Supposedly thread-safe
	 */
	public IterableIterator<Entity> getAllLoadedEntities();

	/* Direct voxel data accessors */
	
	/**
	 * Returns the block data at the specified location
	 * @return The raw block data, see {@link VoxelFormat}
	 */
	public int getVoxelData(Vector3dm location);

	/**
	 * Returns the block data at the specified location
	 * @return The raw block data, see {@link VoxelFormat}
	 */
	public int getVoxelData(int x, int y, int z);
	
	/**
	 * Sets the block data at the specified location
	 * @param data The new data to set the block to, see {@link VoxelFormat}
	 */
	public void setVoxelData(int x, int y, int z, int data);

	/**
	 * Sets the block data at the specified location
	 * @param data The new data to set the block to, see {@link VoxelFormat}
	 */
	public void setVoxelData(Location location, int data);
	
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
	 * Only sets the data, don't trigger any logic
	 */
	public void setVoxelDataWithoutUpdates(int x, int y, int z, int data);
	
	/* Voxel lightning helper functions */
	
	/**
	 * @return The sun light level of the block per {@link VoxelFormat} ( 0-15 ) using either getVoxelDataAt if the chunk is loaded or
	 * the heightmap ( y <= heightmapLevel(x, z) ? 0 : 15 )
	 */
	public int getSunlightLevelWorldCoordinates(int x, int y, int z);
	
	/**
	 * @return The sun light level of the block per {@link VoxelFormat} ( 0-15 ) using either getVoxelDataAt if the chunk is loaded or
	 * the heightmap ( y <= heightmapLevel(x, z) ? 0 : 15 )
	 */
	public int getSunlightLevelLocation(Location location);
	
	/**
	 * @return Returns the block light level of the block per {@link VoxelFormat} ( 0-15 ) using getVoxelDataAt ( if the chunk isn't loaded it will return a zero. )
	 */
	public int getBlocklightLevelWorldCoordinates(int x, int y, int z);

	/**
	 * @return Returns the block light level of the block per {@link VoxelFormat} ( 0-15 ) using getVoxelDataAt ( if the chunk isn't loaded it will return a zero. )
	 */
	public int getBlocklightLevelLocation(Location location);
	
	/* Chunks */
	
	/**
	 * Aquires a ChunkHolder and registers it's user, triggering a load operation for the underlying chunk and preventing it to unload until all the
	 * users either unregisters or gets garbage collected and it's reference nulls out.
	 */
	public ChunkHolder aquireChunkHolderLocation(WorldUser user, Location location);
	
	/**
	 * Aquires a ChunkHolder and registers it's user, triggering a load operation for the underlying chunk and preventing it to unload until all the
	 * users either unregisters or gets garbage collected and it's reference nulls out.
	 */
	public ChunkHolder aquireChunkHolderWorldCoordinates(WorldUser user, int worldX, int worldY, int worldZ);

	/**
	 * Aquires a ChunkHolder and registers it's user, triggering a load operation for the underlying chunk and preventing it to unload until all the
	 * users either unregisters or gets garbage collected and it's reference nulls out.
	 */
	public ChunkHolder aquireChunkHolder(WorldUser user, int chunkX, int chunkY, int chunkZ);
	
	/**
	 * Returns true if a chunk was loaded. Not recommanded nor intended to use as a replacement for a '== null' check after getChunk() because of the load/unload
	 * mechanisms !
	 */
	public boolean isChunkLoaded(int chunkX, int chunkY, int chunkZ);
	
	/**
	 * Returns either null or a valid chunk if a corresponding ChunkHolder was aquired by someone and the chunk had time to load.
	 */
	public Chunk getChunk(int chunkX, int chunkY, int chunkZ);

	/**
	 * Returns either null or a valid chunk if a corresponding ChunkHolder was aquired by someone and the chunk had time to load.
	 */
	public Chunk getChunkWorldCoordinates(int worldX, int worldY, int worldZ);
	
	/**
	 * Returns either null or a valid chunk if a corresponding ChunkHolder was aquired by someone and the chunk had time to load.
	 */
	public Chunk getChunkWorldCoordinates(Location location);
	
	/**
	 * Returns either null or a valid chunk if a corresponding ChunkHolder was aquired by someone and the chunk had time to load.
	 */
	public ChunksIterator getAllLoadedChunks();
	
	/* Regions */
	
	/**
	 * Aquires a region and registers it's user, triggering a load operation for the region and preventing it to unload until all the users
	 *  either unregisters or gets garbage collected and it's reference nulls out.
	 */
	public Region aquireRegion(WorldUser user, int regionX, int regionY, int regionZ);
	
	/**
	 * Aquires a region and registers it's user, triggering a load operation for the region and preventing it to unload until all the users
	 *  either unregisters or gets garbage collected and it's reference nulls out.
	 */
	public Region aquireRegionChunkCoordinates(WorldUser user, int chunkX, int chunkY, int chunkZ);
	
	/**
	 * Aquires a region and registers it's user, triggering a load operation for the region and preventing it to unload until all the users
	 *  either unregisters or gets garbage collected and it's reference nulls out.
	 */
	public Region aquireRegionWorldCoordinates(WorldUser user, int worldX, int worldY, int worldZ);
	
	/**
	 * Aquires a region and registers it's user, triggering a load operation for the region and preventing it to unload until all the users
	 *  either unregisters or gets garbage collected and it's reference nulls out.
	 */
	public Region aquireRegionLocation(WorldUser user, Location location);
	
	/**
	 * Returns either null or a valid, entirely loaded region if the aquireRegion method was called and it had time to load and there is still one user using it
	 */
	public Region getRegion(int regionX, int regionY, int regionZ);

	/**
	 * Returns either null or a valid, entirely loaded region if the aquireRegion method was called and it had time to load and there is still one user using it
	 */
	public Region getRegionChunkCoordinates(int chunkX, int chunkY, int chunkZ);
	
	/**
	 * Returns either null or a valid, entirely loaded region if the aquireRegion method was called and it had time to load and there is still one user using it
	 */
	public Region getRegionWorldCoordinates(int worldX, int worldY, int worldZ);
	
	/**
	 * Returns either null or a valid, entirely loaded region if the aquireRegion method was called and it had time to load and there is still one user using it
	 */
	public Region getRegionLocation(Location location);

	/* Region Summaries */
	
	public RegionSummaries getRegionsSummariesHolder();
	
	/**
	 * For dirty hacks that need so
	 */
	//TODO put that in WorldClient
	public void redrawEverything();

	/**
	 * Blocking method saving all loaded chunks
	 */
	public void saveEverything();

	/**
	 * Destroys the world, kill threads and frees stuff
	 */
	public void destroy();

	public Location getDefaultSpawnLocation();

	/**
	 * Sets the time of the World. By default the time is set at 5000 and it uses a 10.000 cycle, 0 being midnight and 5000 being midday
	 * @param time
	 */
	public void setTime(long time);

	public long getTime();

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

	/**
	 * Game-logic function. Not something you'd be supposed to call
	 */
	public void tick();
	
	public long getTicksElapsed();

	/**
	 * Called when some controllable entity try to interact with the world
	 * @return true if the interaction was handled
	 */
	public boolean handleInteraction(Entity entity, Location blockLocation, Input input);
	
	/* Raytracers and methods to grab entities */
	
	/**
	 * Raytraces throught the world to find a solid block
	 * @param limit Between 0 and a finite number
	 * @return The exact location of the intersection or null if it didn't found one
	 */
	public Location raytraceSolid(Vector3dm initialPosition, Vector3dm direction, double limit);
	
	/**
	 * Raytraces throught the world to find a solid block
	 * @param limit Between 0 and a finite number
	 * @return The exact location of the step just before the intersection ( as to get the adjacent block ) or null if it didn't found one
	 */
	public Location raytraceSolidOuter(Vector3dm initialPosition, Vector3dm direction, double limit);
	
	/**
	 * Raytraces throught the world to find a solid or selectable block
	 * @param limit Between 0 and a finite number
	 * @return The exact location of the intersection or null if it didn't found one
	 */
	public Location raytraceSelectable(Location initialPosition, Vector3dm direction, double limit);
	
	/**
	 * Takes into account the voxel terrain and will stop at a solid block, <b>warning</b> limit can't be == -1 !
	 * @param limit Between 0 and a finite number
	 * @return Returns all entities that intersects with the ray within the limit, ordered nearest to furthest
	 */
	public Iterator<Entity> rayTraceEntities(Vector3dm initialPosition, Vector3dm direction, double limit);

	/**
	 * Ignores any terrain
	 * @param limit Either -1 or between 0 and a finite number
	 * @return Returns all entities that intersects with the ray within the limit, ordered nearest to furthest
	 */
	public Iterator<Entity> raytraceEntitiesIgnoringVoxels(Vector3dm initialPosition, Vector3dm direction, double limit);
	
	/* Various managers */
	
	public DecalsManager getDecalsManager();
	
	public ParticlesManager getParticlesManager();

	public SoundManager getSoundManager();
}