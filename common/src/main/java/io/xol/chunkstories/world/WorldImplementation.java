//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.joml.Vector3dc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.GameLogic;
import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.content.Content;
import io.xol.chunkstories.api.content.ContentTranslator;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityBase;
import io.xol.chunkstories.api.entity.EntityLiving;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.entity.interfaces.EntityNameable;
import io.xol.chunkstories.api.events.player.PlayerSpawnEvent;
import io.xol.chunkstories.api.events.voxel.WorldModificationCause;
import io.xol.chunkstories.api.exceptions.world.ChunkNotLoadedException;
import io.xol.chunkstories.api.exceptions.world.RegionNotLoadedException;
import io.xol.chunkstories.api.exceptions.world.WorldException;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.particles.ParticlesManager;
import io.xol.chunkstories.api.physics.CollisionBox;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.rendering.world.chunk.ChunkRenderable;
import io.xol.chunkstories.api.util.ConfigDeprecated;
import io.xol.chunkstories.api.util.IterableIterator;
import io.xol.chunkstories.api.util.concurrency.Fence;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelSide;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldCollisionsManager;
import io.xol.chunkstories.api.world.WorldInfo;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.api.world.WorldUser;
import io.xol.chunkstories.api.world.cell.Cell;
import io.xol.chunkstories.api.world.cell.CellData;
import io.xol.chunkstories.api.world.cell.FutureCell;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.api.world.chunk.Chunk.ChunkCell;
import io.xol.chunkstories.api.world.chunk.ChunkHolder;
import io.xol.chunkstories.api.world.chunk.ChunksIterator;
import io.xol.chunkstories.api.world.generator.WorldGenerator;
import io.xol.chunkstories.api.world.heightmap.Heightmap;
import io.xol.chunkstories.api.world.region.Region;
import io.xol.chunkstories.content.sandbox.UnthrustedUserContentSecurityManager;
import io.xol.chunkstories.content.translator.AbstractContentTranslator;
import io.xol.chunkstories.content.translator.IncompatibleContentException;
import io.xol.chunkstories.content.translator.InitialContentTranslator;
import io.xol.chunkstories.content.translator.LoadedContentTranslator;
import io.xol.chunkstories.entity.EntityWorldIterator;
import io.xol.chunkstories.entity.SerializedEntityFile;
import io.xol.chunkstories.util.concurrency.CompoundFence;
import io.xol.chunkstories.util.config.OldStyleConfigFile;
import io.xol.chunkstories.world.chunk.CubicChunk;
import io.xol.chunkstories.world.io.IOTasks;
import io.xol.chunkstories.world.iterators.AABBVoxelIterator;
import io.xol.chunkstories.world.iterators.WorldChunksIterator;
import io.xol.chunkstories.world.logic.WorldLogicThread;
import io.xol.chunkstories.world.region.HashMapWorldRegionsHolder;
import io.xol.chunkstories.world.region.RegionImplementation;
import io.xol.chunkstories.world.summary.WorldHeightmapsImplementation;



public abstract class WorldImplementation implements World
{
	protected final GameContext gameContext;
	
	private final ContentTranslator contentTranslator;
	private final AbstractContentTranslator masterContentTranslator;
	
	protected final WorldInfoImplementation worldInfo;
	private final WorldInfoMaster worldInfoMaster;
	
	private final File folder;

	//protected final boolean client;
	private final ConfigDeprecated internalData;

	private WorldGenerator generator;

	// The world age, also tick counter. Can count for billions of real-world
	// time so we are not in trouble.
	// Let's say that the game world runs at 60Ticks per second
	public long worldTicksCounter = 0;

	//Timecycle counter
	private long worldTime = 5000;
	private float overcastFactor = 0.2f;

	//Who does the actual work
	public IOTasks ioHandler;
	private WorldLogicThread worldThread;

	private HashMapWorldRegionsHolder regions;

	// Heightmap management
	private WorldHeightmapsImplementation regionSummaries;

	// Temporary entity list
	protected final WorldEntitiesHolder entities;

	public ReadWriteLock entitiesLock = new ReentrantReadWriteLock(true);

	// Particles
	private ParticlesManager particlesHolder;
	private final WorldCollisionsManager collisionsManager;

	//Entity IDS counter
	AtomicLong entitiesUUIDGenerator = new AtomicLong();
	
	public WorldImplementation(GameContext gameContext, WorldInfoImplementation info) throws WorldLoadingException {
		this(gameContext, info, null);
	}
	
	public WorldImplementation(GameContext gameContext, WorldInfoImplementation info, ContentTranslator initialContentTranslator) throws WorldLoadingException {
		try {
			this.gameContext = gameContext;
			this.worldInfo = info;
	
			//Create holders for the world data
			this.regions = new HashMapWorldRegionsHolder(this);
			this.regionSummaries = new WorldHeightmapsImplementation(this);
			
			//And for the citizens
			this.entities = new WorldEntitiesHolder(this);
	
			if (this instanceof WorldMaster) // Master world initialization
			{
				boolean new_world = false;
				// WorldInfoMaster are backed by a file. If we pass the World constructor a
				// simple WorldInfo, we consider being asked to create a new world
				if(worldInfo instanceof WorldInfoMaster) {
					worldInfoMaster = (WorldInfoMaster)worldInfo;
				} else {
					worldInfoMaster = new WorldInfoMaster(worldInfo);
					worldInfoMaster.save();
					new_world = true;
				}
				
				//Obtain the parent folder
				this.folder = worldInfoMaster.getFile().getParentFile();
				
				//Check for an existing content translator
				File contentTranslatorFile = new File(folder.getPath()+"/content_mappings.dat");
				if(contentTranslatorFile.exists()) {
					this.masterContentTranslator = LoadedContentTranslator.loadFromFile(gameContext.getContent(), contentTranslatorFile);
				} else {
					if(!new_world) {
						//Legacy world! Use the default mappings from when ids where dynamically allocated.
						logger.warn("Loading a legacy (pre-automagic ids allocation), trying default mappings...");
						InputStream is = getClass().getResourceAsStream("/legacy_mappings.dat");
						this.masterContentTranslator = new LoadedContentTranslator(gameContext.getContent(), new BufferedReader(new InputStreamReader(is)));
						this.masterContentTranslator.test();
					} else {
						//Build a new content translator
						this.masterContentTranslator = new InitialContentTranslator(gameContext.getContent());
					}
				}
				
				//Copy the reference of the loaded content translator and save it immediately
				this.contentTranslator = this.masterContentTranslator;
				this.masterContentTranslator.save(new File(this.getFolderPath() + "/content_mappings.dat"));

				File internalDatFile = new File(folder.getPath()+"/internal.dat");
				this.internalData = new OldStyleConfigFile(internalDatFile.getAbsolutePath());
				this.internalData.load();
	
				this.entitiesUUIDGenerator.set(internalData.getLong("entities-ids-counter", 0));
				this.worldTime = internalData.getLong("worldTime", 5000);
				this.worldTicksCounter = internalData.getLong("worldTimeInternal", 0);
				this.overcastFactor = internalData.getFloat("overcastFactor", 0.2f);
			}
			// Slave world initialization
			else {
				if(initialContentTranslator == null) {
					throw new WorldLoadingException("No ContentTranslator providen and none could be found on disk since this is a Slave World.");
				} else {
					this.contentTranslator = initialContentTranslator;
				}
				
				//Null-out final fields meant for master worlds
				this.worldInfoMaster = null;
				this.folder = null;
				this.internalData = null;
				this.masterContentTranslator = null;
			}
			
			this.generator = gameContext.getContent().generators().getWorldGenerator(info.getGeneratorName()).createForWorld(this);
			this.collisionsManager = new DefaultWorldCollisionsManager(this);
			
			//Start the world logic thread
			this.worldThread = new WorldLogicThread(this, new UnthrustedUserContentSecurityManager());
		} catch(IOException e) {
			throw new WorldLoadingException("Couldn't load world ", e);
		} catch (IncompatibleContentException e) {
			throw new WorldLoadingException("Couldn't load world ", e);
		}
	}

	public void startLogic()
	{
		worldThread.start();
	}
	
	public Fence stopLogic() {
		return worldThread.stopLogicThread();
	}

	@Override
	public WorldInfo getWorldInfo()
	{
		return worldInfo;
	}

	public File getFolderFile()
	{
		return folder;
	}

	/**
	 * Returns where this world resides on actual disk
	 * 
	 * @return
	 */
	public String getFolderPath()
	{
		if (folder != null)
			return folder.getAbsolutePath();
		return null;
	}

	public void spawnPlayer(Player player)
	{
		if(!(this instanceof WorldMaster))
			throw new UnsupportedOperationException("Only Master Worlds can do this");
		
		Entity savedEntity = null;
		
		SerializedEntityFile playerEntityFile = new SerializedEntityFile(this.getFolderPath() + "/players/" + player.getName().toLowerCase() + ".csf");
		if(playerEntityFile.exists())
			savedEntity = playerEntityFile.read(this);
		
		Location previousLocation = null;
		if(savedEntity != null)
			previousLocation = savedEntity.getLocation();
		
		PlayerSpawnEvent playerSpawnEvent = new PlayerSpawnEvent(player, (WorldMaster) this, savedEntity, previousLocation);
		getGameContext().getPluginManager().fireEvent(playerSpawnEvent);
		
		if(!playerSpawnEvent.isCancelled())
		{
			Entity entity = playerSpawnEvent.getEntity();
			
			Location actualSpawnLocation = playerSpawnEvent.getSpawnLocation();
			if(actualSpawnLocation == null)
				actualSpawnLocation = this.getDefaultSpawnLocation();
			
			//TODO EntitySimplePlayer ?
			if(entity == null || ((entity instanceof EntityLiving) && (((EntityLiving) entity).isDead())))
				entity = this.gameContext.getContent().entities().getEntityDefinition("player").create(actualSpawnLocation);
				//entity = new EntityPlayer(this, 0d, 0d, 0d, player.getName()); //Default entity
			else
				entity.setUUID(-1);
			
			//Name your player !
			if(entity instanceof EntityNameable)
				((EntityNameable)entity).getNameComponent().setName(player.getName());
			
			entity.setLocation(actualSpawnLocation);
			
			addEntity(entity);
			if(entity instanceof EntityControllable)
				player.setControlledEntity((EntityControllable) entity);
			else
				System.out.println("Error : entity is not controllable");
		}
	}

	@Override
	public void addEntity(final Entity entity)
	{
		//Assign an UUID to entities lacking one
		if (this instanceof WorldMaster && entity.getUUID() == -1)
		{
			long nextUUID = nextEntityId();
			entity.setUUID(nextUUID);
		}

		Entity check = this.getEntityByUUID(entity.getUUID());
		if (check != null)
		{
			logger().error("Added an entity twice "+check+" conflits with "+entity + " UUID: "+entity.getUUID());
			//logger().save();
			Thread.dumpStack();
			return;//System.exit(-1);
		}

		//Add it to the world
		((EntityBase) entity).markHasSpawned();
		
		assert entity.getWorld() == this;

		Chunk chunk = this.getChunkWorldCoordinates(entity.getLocation());
		if(chunk != null) {
			((EntityBase)entity).positionComponent.trySnappingToChunk();
		}
		
		this.entities.insertEntity(entity);
	}

	@Override
	public boolean removeEntity(Entity entity)
	{
		try {
			entitiesLock.writeLock().lock();
			if (entity != null)
			{
				EntityBase ent = (EntityBase)entity;

				//Only once
				if (ent.getComponentExistence().exists())
				{
					//Destroys it
					ent.getComponentExistence().destroyEntity();

					//Removes it's reference within the region
					if (ent.positionComponent.getChunkWithin() != null)
						ent.positionComponent.getChunkWithin().removeEntity(entity);

					//Actually removes it from the world list
					removeEntityFromList(entity);

					return true;
				}
			}

			return false;
		}
		finally {
			entitiesLock.writeLock().unlock();
		}
	}

	@Override
	public boolean removeEntityByUUID(long uuid)
	{
		Entity entityFound = this.getEntityByUUID(uuid);

		if (entityFound != null)
			return removeEntity(entityFound);

		return false;
	}

	/**
	 * Internal methods that actually removes the entity from the list after having removed it's reference from elsewere.
	 * 
	 * @return
	 */
	public boolean removeEntityFromList(Entity entity)
	{
		//Remove the entity from the world first
		boolean result = entities.removeEntity(entity);
		
		//Tell anyone still subscribed to this entity to sod off
		((EntityBase)entity).getAllSubscribers().forEach(subscriber -> { subscriber.unsubscribe(entity); });
		return result;
	}

	@Override
	public void tick()
	{
		//Iterates over every entity
		try
		{
			entitiesLock.writeLock().lock();
			Iterator<Entity> iter = this.getAllLoadedEntities();
			Entity entity;
			while (iter.hasNext())
			{
				entity = iter.next();

				//Check entity's region is loaded
				if (entity.getChunk() != null)
					entity.tick();
				
				//Tries to snap the entity to the region if it ends up being loaded
				else
					((EntityBase)entity).positionComponent.trySnappingToChunk();

			}
		} finally {
			entitiesLock.writeLock().unlock();
		}

		//Increase the ticks counter
		worldTicksCounter++;

		//Time cycle
		if (this instanceof WorldMaster && internalData.getBoolean("doTimeCycle", true))
			if (worldTicksCounter % 60 == 0)
				worldTime++;
	}

	@Override
	public IterableIterator<Entity> getAllLoadedEntities()
	{
		return new EntityWorldIterator(entities.iterator());
	}
	
	public NearEntitiesIterator getEntitiesInBox(Vector3dc center, Vector3dc boxSize)
	{
		return entities.getEntitiesInBox(center, boxSize);
	}

	@Override
	public Entity getEntityByUUID(long entityID)
	{
		return entities.getEntityByUUID(entityID);
	}

	@Override
	public WorldHeightmapsImplementation getRegionsSummariesHolder()
	{
		return regionSummaries;
	}
	
	@Override
	public ChunkCell peek(Vector3dc location) throws WorldException
	{
		return peek((int) (double) location.x(), (int) (double) location.y(), (int) (double) location.z());
	}
	
	@Override
	/** Fancy get method that throws exceptions when the world isn't loaded */
	public ChunkCell peek(int x, int y, int z) throws WorldException {
		x = sanitizeHorizontalCoordinate(x);
		y = sanitizeVerticalCoordinate(y);
		z = sanitizeHorizontalCoordinate(z);
		
		Region region = this.getRegionWorldCoordinates(x, y, z);
		if(region == null)
			throw new RegionNotLoadedException(this, x / 256, y / 256, z / 256);
			
		Chunk chunk = region.getChunk((x / 32) % 8, (y / 32) % 8, (z / 32) % 8);
		if(chunk == null)
			throw new ChunkNotLoadedException(region, (x / 32) % 8, (y / 32) % 8, (z / 32) % 8);
		
		return chunk.peek(x, y, z);
	}

	@Override
	public WorldCell peekSafely(int x, int y, int z) {
		
		x = sanitizeHorizontalCoordinate(x);
		y = sanitizeVerticalCoordinate(y);
		z = sanitizeHorizontalCoordinate(z);
		
		Region region = this.getRegionWorldCoordinates(x, y, z);
		if(region == null) {
			return new UnloadedWorldCell(x, y, z, this.getGameContext().getContent().voxels().air(), 0, 0, 0);
		}
			
		Chunk chunk = region.getChunk((x / 32) % 8, (y / 32) % 8, (z / 32) % 8);
		if(chunk == null)
			return new UnloadedWorldCell(x, y, z, this.getGameContext().getContent().voxels().air(), 0, 0, 0);
		
		return chunk.peek(x, y, z);
	}

	/** Safety: provide an alternative 'fake' cell if the proper one isn't loaded */
	class UnloadedWorldCell extends Cell implements WorldCell {

		public UnloadedWorldCell(int x, int y, int z, Voxel voxel, int meta, int blocklight, int sunlight) {
			super(x, y, z, voxel, meta, blocklight, sunlight);
			
			int groundHeight = getRegionsSummariesHolder().getHeightAtWorldCoordinates(x, z);
			if(groundHeight < y && groundHeight != Heightmap.NO_DATA)
				this.sunlight = 15;
		}

		@Override
		public CellData getNeightbor(int side_int) {
			VoxelSide side = VoxelSide.values()[side_int];
			return peekSafely(getX() + side.dx, getY() + side.dy, getZ() + side.dz);
		}

		@Override
		public World getWorld() {
			return WorldImplementation.this;
		}

		@Override
		public void setVoxel(Voxel voxel) {
			logger.warn("Trying to edit a UnloadedWorldCell." + this);
		}

		@Override
		public void setMetaData(int metadata) {
			logger.warn("Trying to edit a UnloadedWorldCell." + this);
		}

		@Override
		public void setSunlight(int sunlight) {
			logger.warn("Trying to edit a UnloadedWorldCell." + this);
		}

		@Override
		public void setBlocklight(int blocklight) {
			logger.warn("Trying to edit a UnloadedWorldCell." + this);
		}
	}
	
	@Override
	public WorldCell peekSafely(Vector3dc location) {
		return peekSafely((int) (double) location.x(), (int) (double) location.y(), (int) (double) location.z());
	}

	@Override
	public Voxel peekSimple(int x, int y, int z) {
		x = sanitizeHorizontalCoordinate(x);
		y = sanitizeVerticalCoordinate(y);
		z = sanitizeHorizontalCoordinate(z);
		
		Chunk chunk = this.getChunkWorldCoordinates(x, y, z);
		if(chunk == null)
			return gameContext.getContent().voxels().air();
		else
			return chunk.peekSimple(x, y, z);
	}
	
	@Override
	public int peekRaw(int x, int y, int z) {
		x = sanitizeHorizontalCoordinate(x);
		y = sanitizeVerticalCoordinate(y);
		z = sanitizeHorizontalCoordinate(z);
		
		Chunk chunk = this.getChunkWorldCoordinates(x, y, z);
		if(chunk == null)
			return 0x00000000;
		else
			return chunk.peekRaw(x, y, z);
	}

	@Override
	public ChunkCell poke(int x, int y, int z, Voxel voxel, int sunlight, int blocklight, int metadata, WorldModificationCause cause)
			throws WorldException {
		x = sanitizeHorizontalCoordinate(x);
		y = sanitizeVerticalCoordinate(y);
		z = sanitizeHorizontalCoordinate(z);
		
		Region region = this.getRegionWorldCoordinates(x, y, z);
		if(region == null)
			throw new RegionNotLoadedException(this, x / 256, y / 256, z / 256);
			
		Chunk chunk = region.getChunk((x / 32) % 8, (y / 32) % 8, (z / 32) % 8);
		if(chunk == null)
			throw new ChunkNotLoadedException(region, (x / 32) % 8, (y / 32) % 8, (z / 32) % 8);
		
		return chunk.poke(x, y, z, voxel, sunlight, blocklight, metadata, cause);
	}
	
	@Override
	public ChunkCell poke(FutureCell future, WorldModificationCause cause) throws WorldException {
		return poke(future.getX(), future.getY(), future.getZ(), future.getVoxel(), future.getSunlight(), future.getBlocklight(), future.getMetaData(), cause);
	}

	@Override
	public void pokeSimple(int x, int y, int z, Voxel voxel, int sunlight, int blocklight, int metadata) {
		x = sanitizeHorizontalCoordinate(x);
		y = sanitizeVerticalCoordinate(y);
		z = sanitizeHorizontalCoordinate(z);
		
		Chunk chunk = this.getChunkWorldCoordinates(x, y, z);
		if(chunk != null)
			chunk.pokeSimple(x, y, z, voxel, sunlight, blocklight, metadata);
	}
	
	@Override
	public void pokeSimple(FutureCell future) {
		pokeSimple(future.getX(), future.getY(), future.getZ(), future.getVoxel(), future.getSunlight(), future.getBlocklight(), future.getMetaData());
	}

	@Override
	public void pokeSimpleSilently(int x, int y, int z, Voxel voxel, int sunlight, int blocklight, int metadata) {
		x = sanitizeHorizontalCoordinate(x);
		y = sanitizeVerticalCoordinate(y);
		z = sanitizeHorizontalCoordinate(z);
		
		Chunk chunk = this.getChunkWorldCoordinates(x, y, z);
		if(chunk != null)
			chunk.pokeSimpleSilently(x, y, z, voxel, sunlight, blocklight, metadata);
	}
	
	@Override
	public void pokeSimpleSilently(FutureCell future) {
		pokeSimpleSilently(future.getX(), future.getY(), future.getZ(), future.getVoxel(), future.getSunlight(), future.getBlocklight(), future.getMetaData());
	}
	
	@Override
	public void pokeRaw(int x, int y, int z, int raw_data) {
		x = sanitizeHorizontalCoordinate(x);
		y = sanitizeVerticalCoordinate(y);
		z = sanitizeHorizontalCoordinate(z);
		
		Chunk chunk = this.getChunkWorldCoordinates(x, y, z);
		if(chunk != null)
			chunk.pokeRaw(x, y, z, raw_data);
	}
	
	@Override
	public void pokeRawSilently(int x, int y, int z, int raw_data) {
		x = sanitizeHorizontalCoordinate(x);
		y = sanitizeVerticalCoordinate(y);
		z = sanitizeHorizontalCoordinate(z);
		
		Chunk chunk = this.getChunkWorldCoordinates(x, y, z);
		if(chunk != null)
			chunk.pokeRawSilently(x, y, z, raw_data);
	}
	
	@Override
	public IterableIterator<CellData> getVoxelsWithin(CollisionBox boundingBox) {
		return new AABBVoxelIterator(this, boundingBox);
	}

	@Override
	//TODO move to client
	public synchronized void redrawEverything()
	{
		ChunksIterator i = this.getAllLoadedChunks();
		Chunk c;
		while (i.hasNext())
		{
			c = i.next();
			if (c instanceof ChunkRenderable)
			{
				ChunkRenderable c2 = (ChunkRenderable) c;
				c2.lightBaker().requestLightningUpdate();
				c2.meshUpdater().requestMeshUpdate();
			}
		}
	}

	@Override
	public Fence saveEverything()
	{
		CompoundFence ioOperationsFence = new CompoundFence();
		
		logger.info("Saving all parts of world "+worldInfo.getName());
		ioOperationsFence.add(regions.saveAll());
		ioOperationsFence.add(getRegionsSummariesHolder().saveAllLoadedSummaries());

		if(worldInfoMaster != null)
			this.worldInfoMaster.save();
		
		this.internalData.setLong("entities-ids-counter", entitiesUUIDGenerator.get());
		this.internalData.setLong("worldTime", worldTime);
		this.internalData.setLong("worldTimeInternal", worldTicksCounter);
		this.internalData.setFloat("overcastFactor", overcastFactor);
		this.internalData.save();
		
		if(masterContentTranslator != null)
			this.masterContentTranslator.save(new File(this.getFolderPath() + "/content_mappings.dat"));
		
		return ioOperationsFence;
	}

	@Override
	public float getWeather()
	{
		return overcastFactor;
	}

	public long nextEntityId()
	{
		return entitiesUUIDGenerator.getAndIncrement();
	}

	@Override
	public void setWeather(float overcastFactor)
	{
		this.overcastFactor = overcastFactor;
	}

	@Override
	public Location getDefaultSpawnLocation()
	{
		double dx = internalData.getDouble("defaultSpawnX", 0.0);
		double dy = internalData.getDouble("defaultSpawnY", 100.0);
		double dz = internalData.getDouble("defaultSpawnZ", 0.0);
		return new Location(this, dx, dy, dz);
	}

	@Override
	public void setDefaultSpawnLocation(Location location)
	{
		internalData.setDouble("defaultSpawnX", location.x());
		internalData.setDouble("defaultSpawnY", location.y());
		internalData.setDouble("defaultSpawnZ", location.z());
		internalData.save();
	}

	@Override
	public void setTime(long time)
	{
		this.worldTime = time;
	}

	@Override
	public WorldGenerator getGenerator()
	{
		return generator;
	}

	@Override
	public boolean handleInteraction(Entity entity, Location voxelLocation, Input input)
	{
		if (voxelLocation == null)
			return false;

		CellData peek;
		try {
			peek = this.peek(voxelLocation);
		} catch (WorldException e) {
			// Will not accept interacting with unloaded blocks
			return false;
		}
		
		return peek.getVoxel().handleInteraction(entity, (ChunkCell) peek, input);
	}

	private int sanitizeHorizontalCoordinate(int coordinate)
	{
		coordinate = coordinate % (getSizeInChunks() * 32);
		if (coordinate < 0)
			coordinate += getSizeInChunks() * 32;
		return coordinate;
	}

	private int sanitizeVerticalCoordinate(int coordinate)
	{
		if (coordinate < 0)
			coordinate = 0;
		if (coordinate >= worldInfo.getSize().heightInChunks * 32)
			coordinate = worldInfo.getSize().heightInChunks * 32 - 1;
		return coordinate;
	}
	
	@Override
	public WorldCollisionsManager collisionsManager()
	{
		return collisionsManager;
	}

	@Override
	public ParticlesManager getParticlesManager()
	{
		return particlesHolder;
	}

	@Override
	public ChunkHolder aquireChunkHolderLocation(WorldUser user, Location location)
	{
		return aquireChunkHolder(user, (int) (double) location.x(), (int) (double) location.y(), (int) (double) location.z());
	}

	@Override
	public ChunkHolder aquireChunkHolder(WorldUser user, int chunkX, int chunkY, int chunkZ)
	{
		//Sanitation of input data
		chunkX = chunkX % getSizeInChunks();
		chunkZ = chunkZ % getSizeInChunks();
		if (chunkX < 0)
			chunkX += getSizeInChunks();
		if (chunkZ < 0)
			chunkZ += getSizeInChunks();
		return this.getRegionsHolder().aquireChunkHolder(user, chunkX, chunkY, chunkZ);
	}

	@Override
	public ChunkHolder aquireChunkHolderWorldCoordinates(WorldUser user, int worldX, int worldY, int worldZ)
	{
		worldX = sanitizeHorizontalCoordinate(worldX);
		worldY = sanitizeVerticalCoordinate(worldY);
		worldZ = sanitizeHorizontalCoordinate(worldZ);

		return this.getRegionsHolder().aquireChunkHolder(user, worldX / 32, worldY / 32, worldZ / 32);
	}

	@Override
	public boolean isChunkLoaded(int chunkX, int chunkY, int chunkZ)
	{
		//Sanitation of input data
		chunkX = chunkX % getSizeInChunks();
		chunkZ = chunkZ % getSizeInChunks();
		if (chunkX < 0)
			chunkX += getSizeInChunks();
		if (chunkZ < 0)
			chunkZ += getSizeInChunks();
		//Out of bounds checks
		if (chunkY < 0)
			return false;
		if (chunkY >= worldInfo.getSize().heightInChunks)
			return false;
		//If it doesn't return null then it exists
		return this.regions.getChunk(chunkX, chunkY, chunkZ) != null;
	}

	@Override
	public CubicChunk getChunkWorldCoordinates(Location location)
	{
		return getChunkWorldCoordinates((int) (double) location.x(), (int) (double) location.y(), (int) (double) location.z());
	}

	@Override
	public CubicChunk getChunkWorldCoordinates(int worldX, int worldY, int worldZ)
	{
		return getChunk(worldX / 32, worldY / 32, worldZ / 32);
	}

	@Override
	public CubicChunk getChunk(int chunkX, int chunkY, int chunkZ)
	{
		chunkX = chunkX % getSizeInChunks();
		chunkZ = chunkZ % getSizeInChunks();
		if (chunkX < 0)
			chunkX += getSizeInChunks();
		if (chunkZ < 0)
			chunkZ += getSizeInChunks();
		if (chunkY < 0)
			return null;
		if (chunkY >= worldInfo.getSize().heightInChunks)
			return null;
		return regions.getChunk(chunkX, chunkY, chunkZ);
	}

	@Override
	public ChunksIterator getAllLoadedChunks()
	{
		return new WorldChunksIterator(this);
	}

	public HashMapWorldRegionsHolder getRegionsHolder()
	{
		return regions;
	}

	@Override
	public RegionImplementation aquireRegion(WorldUser user, int regionX, int regionY, int regionZ)
	{
		return this.getRegionsHolder().aquireRegion(user, regionX, regionY, regionZ);
	}

	@Override
	public RegionImplementation aquireRegionChunkCoordinates(WorldUser user, int chunkX, int chunkY, int chunkZ)
	{
		return aquireRegion(user, chunkX / 8, chunkY / 8, chunkZ / 8);
	}

	@Override
	public RegionImplementation aquireRegionWorldCoordinates(WorldUser user, int worldX, int worldY, int worldZ)
	{
		worldX = sanitizeHorizontalCoordinate(worldX);
		worldY = sanitizeVerticalCoordinate(worldY);
		worldZ = sanitizeHorizontalCoordinate(worldZ);

		return aquireRegion(user, worldX / 256, worldY / 256, worldZ / 256);
	}

	@Override
	public RegionImplementation aquireRegionLocation(WorldUser user, Location location)
	{
		return aquireRegionWorldCoordinates(user, (int) (double) location.x(), (int) (double) location.y(), (int) (double) location.z());
	}

	@Override
	public RegionImplementation getRegionLocation(Location location)
	{
		return getRegionWorldCoordinates((int) (double) location.x(), (int) (double) location.y(), (int) (double) location.z());
	}

	@Override
	public RegionImplementation getRegionWorldCoordinates(int worldX, int worldY, int worldZ)
	{
		worldX = sanitizeHorizontalCoordinate(worldX);
		worldY = sanitizeVerticalCoordinate(worldY);
		worldZ = sanitizeHorizontalCoordinate(worldZ);

		return getRegion(worldX / 256, worldY / 256, worldZ / 256);
	}

	@Override
	public RegionImplementation getRegionChunkCoordinates(int chunkX, int chunkY, int chunkZ)
	{
		return getRegion(chunkX / 8, chunkY / 8, chunkZ / 8);
	}

	@Override
	public RegionImplementation getRegion(int regionX, int regionY, int regionZ)
	{
		return regions.getRegion(regionX, regionY, regionZ);
	}

	@Override
	public long getTime()
	{
		return worldTime;
	}

	@Override
	public long getTicksElapsed()
	{
		return this.worldTicksCounter;
	}

	@Override
	public GameLogic getGameLogic()
	{
		return worldThread;
	}
	
	@Override
	public GameContext getGameContext()
	{
		return gameContext;
	}
	
	@Override
	public Content getContent() {
		return gameContext.getContent();
	}

	@Override
	public ContentTranslator getContentTranslator() {
		return contentTranslator;
	}

	@Override
	public void destroy()
	{
		//Stop the game logic first
		worldThread.stopLogicThread().traverse();

		this.regions.destroy();
		this.getRegionsSummariesHolder().destroy();
		
		//Always, ALWAYS save this.
		if (this instanceof WorldMaster)
		{
			this.internalData.setLong("entities-ids-counter", entitiesUUIDGenerator.get());
			this.internalData.save();
		}

		//Kill the IO handler
		ioHandler.kill();
	}

	//TODO remove completely ?
	/*public Fence unloadUselessData()
	{
		Fence onlyThisHasAFence = this.getRegionsHolder().unloadsUselessData();
		//this.getRegionsSummariesHolder().unloadsUselessData();
		
		return onlyThisHasAFence;
		//return new TrivialFence();
	}*/

	private static final Logger logger = LoggerFactory.getLogger("world");
	public Logger logger() {
		return logger;
	}
}
