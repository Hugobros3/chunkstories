package io.xol.chunkstories.world;

import java.io.File;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.GameLogic;
import io.xol.chunkstories.api.Location;
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
import org.joml.Vector3dc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.particles.ParticlesManager;
import io.xol.chunkstories.api.physics.CollisionBox;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.rendering.world.ChunkRenderable;
import io.xol.chunkstories.api.util.ConfigDeprecated;
import io.xol.chunkstories.api.util.IterableIterator;
import io.xol.chunkstories.api.util.concurrency.Fence;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelInteractive;
import io.xol.chunkstories.api.world.generator.WorldGenerator;
import io.xol.chunkstories.api.world.WorldInfo;
import io.xol.chunkstories.api.world.VoxelContext;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldCollisionsManager;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.api.world.chunk.Chunk.ChunkVoxelContext;
import io.xol.chunkstories.api.world.chunk.ChunkHolder;
import io.xol.chunkstories.api.world.chunk.WorldUser;
import io.xol.chunkstories.api.world.dummy.DummyContext;
import io.xol.chunkstories.api.world.chunk.ChunksIterator;
import io.xol.chunkstories.api.world.chunk.DummyChunk;
import io.xol.chunkstories.api.world.chunk.Region;
import io.xol.chunkstories.content.sandbox.WorldLogicThread;
import io.xol.chunkstories.content.sandbox.UnthrustedUserContentSecurityManager;
import io.xol.chunkstories.entity.EntityWorldIterator;
import io.xol.chunkstories.entity.SerializedEntityFile;
import io.xol.chunkstories.world.chunk.CubicChunk;
import io.xol.chunkstories.world.io.IOTasks;
import io.xol.chunkstories.world.iterators.AABBVoxelIterator;
import io.xol.chunkstories.world.iterators.WorldChunksIterator;
import io.xol.chunkstories.world.region.RegionImplementation;
import io.xol.chunkstories.world.region.HashMapWorldRegionsHolder;
import io.xol.chunkstories.world.summary.WorldRegionSummariesHolder;
import io.xol.engine.concurrency.CompoundFence;
import io.xol.engine.misc.ConfigFile;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public abstract class WorldImplementation implements World
{
	protected WorldInfoImplementation worldInfo;
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
	private WorldRegionSummariesHolder regionSummaries;

	// Temporary entity list
	protected final EntitiesHolder entities;

	public ReadWriteLock entitiesLock = new ReentrantReadWriteLock(true);

	// Particles
	private ParticlesManager particlesHolder;
	private final WorldCollisionsManager collisionsManager;

	//Entity IDS counter
	AtomicLong entitiesUUIDGenerator = new AtomicLong();

	protected final GameContext gameContext;
	
	public WorldImplementation(GameContext gameContext, WorldInfoImplementation info)
	{
		this.gameContext = gameContext;
		this.worldInfo = info;
		
		//Creates world generator
		this.generator = gameContext.getContent().generators().getWorldGenerator(info.getGeneratorName()).createForWorld(this);

		//Create holders for the world data
		this.regions = new HashMapWorldRegionsHolder(this);
		this.regionSummaries = new WorldRegionSummariesHolder(this);
		
		//And for the citizens
		this.entities = new EntitiesHolder(this);

		if (this instanceof WorldMaster)
		{
			if(!(worldInfo instanceof WorldInfoFile)) {
				throw new RuntimeException("Master worlds can only run off WorldInfoFiles.");
			}
			
			WorldInfoFile worldInfoFile = (WorldInfoFile)worldInfo;
			
			this.folder = worldInfoFile.getFile().getParentFile();//new File(GameDirectory.getGameFolderPath() + "/worlds/" + worldInfo.getInternalName());

			System.out.println(folder.getPath());
			System.out.println(folder.getAbsolutePath());
			System.out.println(folder);
			
			this.internalData = new ConfigFile(this.folder.getPath()+"/internal.dat");//GameDirectory.getGameFolderPath() + "/worlds/" + worldInfo.getInternalName() + "/internal.dat");
			this.internalData.load();

			this.entitiesUUIDGenerator.set(internalData.getLong("entities-ids-counter", 0));
			this.worldTime = internalData.getLong("worldTime", 5000);
			this.worldTicksCounter = internalData.getLong("worldTimeInternal", 0);
			this.overcastFactor = internalData.getFloat("overcastFactor", 0.2f);
		}
		else
		{
			this.folder = null;
			this.internalData = null;
		}

		collisionsManager = new BuiltInWorldCollisionsManager(this);
		
		//Start the world logic thread
		worldThread = new WorldLogicThread(this, new UnthrustedUserContentSecurityManager());
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
				entity = this.gameContext.getContent().entities().getEntityTypeByName("player").create(actualSpawnLocation);
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
			System.exit(-1);
		}

		//Add it to the world
		((EntityBase) entity).markHasSpawned();
		
		assert entity.getWorld() == this;

		Chunk chunk = this.getChunkWorldCoordinates(entity.getLocation());
		if(chunk != null) {
			((EntityBase)entity).positionComponent.trySnappingToChunk();
		}
		
		this.entities.insertEntity(entity);

		//System.out.println("added " + entity + "to the worlde");
	}

	@Override
	public boolean removeEntity(Entity entity)
	{
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
		return entities.removeEntity(entity);
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
		}
		finally
		{
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
		/*Iterator<Entity> ie = getAllLoadedEntities();
		Entity e;
		while (ie.hasNext())
		{
			e = ie.next();
			if (e.getUUID() == entityID)
				return e;
		}
		return null;*/
	}

	@Override
	public WorldRegionSummariesHolder getRegionsSummariesHolder()
	{
		return regionSummaries;
	}

	/*@Override
	public int getVoxelData(Vector3dc location)
	{
		return getVoxelData((int) (double) location.x(), (int) (double) location.y(), (int) (double) location.z());
	}

	@Override
	public int getVoxelData(int x, int y, int z)
	{
		x = sanitizeHorizontalCoordinate(x);
		y = sanitizeVerticalCoordinate(y);
		z = sanitizeHorizontalCoordinate(z);

		Chunk c = regions.getChunk(x / 32, y / 32, z / 32);

		if (c != null)
			return c.getVoxelData(x, y, z);
		return 0;
	}*/
	
	@Override
	public ChunkVoxelContext peek(Vector3dc location) throws WorldException
	{
		return peek((int) (double) location.x(), (int) (double) location.y(), (int) (double) location.z());
	}
	
	@Override
	/** Fancy get method that throws exceptions when the world isn't loaded */
	public ChunkVoxelContext peek(int x, int y, int z) throws WorldException {
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
	public WorldVoxelContext peekSafely(int x, int y, int z) {
		
		x = sanitizeHorizontalCoordinate(x);
		y = sanitizeVerticalCoordinate(y);
		z = sanitizeHorizontalCoordinate(z);
		
		Region region = this.getRegionWorldCoordinates(x, y, z);
		if(region == null)
			return new DummyContext(new DummyChunk(), x, y, z, VoxelFormat.format(01, 0, this.getRegionsSummariesHolder().getHeightAtWorldCoordinates(x, z) >= y ? 0: 15, 0)) {
			@Override
			public Voxel getVoxel() {
				return getGameContext().getContent().voxels().getVoxelById(0);
			}
		};
			
		Chunk chunk = region.getChunk((x / 32) % 8, (y / 32) % 8, (z / 32) % 8);
		if(chunk == null)
			return new DummyContext(new DummyChunk(), x, y, z, VoxelFormat.format(01, 0, this.getRegionsSummariesHolder().getHeightAtWorldCoordinates(x, z) >= y ? 0: 15, 0)) {
			
			@Override
			public Voxel getVoxel() {
				return getGameContext().getContent().voxels().getVoxelById(0);
			}
		};
		
		return chunk.peek(x, y, z);
	}

	@Override
	public WorldVoxelContext peekSafely(Vector3dc location) {
		return peekSafely((int) (double) location.x(), (int) (double) location.y(), (int) (double) location.z());
	}

	@Override
	public int peekSimple(int x, int y, int z) {
		x = sanitizeHorizontalCoordinate(x);
		y = sanitizeVerticalCoordinate(y);
		z = sanitizeHorizontalCoordinate(z);
		
		Chunk chunk = this.getChunkWorldCoordinates(x, y, z);
		if(chunk == null)
			return 0;
		else
			return chunk.peekSimple(x, y, z);
	}

	@Override
	public WorldVoxelContext poke(int x, int y, int z, int newVoxelData, WorldModificationCause cause)
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
		
		return chunk.poke(x, y, z, newVoxelData, cause);
	}

	@Override
	public WorldVoxelContext pokeSilently(int x, int y, int z, int newVoxelData) throws WorldException {
		x = sanitizeHorizontalCoordinate(x);
		y = sanitizeVerticalCoordinate(y);
		z = sanitizeHorizontalCoordinate(z);
		
		Region region = this.getRegionWorldCoordinates(x, y, z);
		if(region == null)
			throw new RegionNotLoadedException(this, x / 256, y / 256, z / 256);
			
		Chunk chunk = region.getChunk((x / 32) % 8, (y / 32) % 8, (z / 32) % 8);
		if(chunk == null)
			throw new ChunkNotLoadedException(region, (x / 32) % 8, (y / 32) % 8, (z / 32) % 8);
		
		return chunk.pokeSilently(x, y, z, newVoxelData);
	}

	@Override
	public void pokeSimple(int x, int y, int z, int newVoxelData) {
		x = sanitizeHorizontalCoordinate(x);
		y = sanitizeVerticalCoordinate(y);
		z = sanitizeHorizontalCoordinate(z);
		
		Chunk chunk = this.getChunkWorldCoordinates(x, y, z);
		if(chunk != null)
			chunk.pokeSimple(x, y, z, newVoxelData);
	}

	@Override
	public void pokeSimpleSilently(int x, int y, int z, int newVoxelData) {
		x = sanitizeHorizontalCoordinate(x);
		y = sanitizeVerticalCoordinate(y);
		z = sanitizeHorizontalCoordinate(z);
		
		Chunk chunk = this.getChunkWorldCoordinates(x, y, z);
		if(chunk != null)
			chunk.pokeSimpleSilently(x, y, z, newVoxelData);
	}

	/*@Override
	public WorldVoxelContext peek(int x, int y, int z)
	{
		return new WorldVoxelContext() {
			
			int data = getVoxelData(x, y, z);
			
			@Override
			public Voxel getVoxel()
			{
				return getGameContext().getContent().voxels().getVoxelById(data);
			}

			@Override
			public int getData()
			{
				return data;
			}

			@Override
			public int getX()
			{
				return x;
			}

			@Override
			public int getY()
			{
				return y;
			}

			@Override
			public int getZ()
			{
				return z;
			}

			@Override
			public int getNeightborData(int side)
			{
				switch (side)
				{
				case (0):
					return getVoxelData(x - 1, y, z);
				case (1):
					return getVoxelData(x, y, z + 1);
				case (2):
					return getVoxelData(x + 1, y, z);
				case (3):
					return getVoxelData(x, y, z - 1);
				case (4):
					return getVoxelData(x, y + 1, z);
				case (5):
					return getVoxelData(x, y - 1, z);
				}
				throw new RuntimeException("Fuck off");
			}

			@Override
			public World getWorld()
			{
				return WorldImplementation.this;
			}

			@Override
			public Location getLocation()
			{
				return new Location(WorldImplementation.this, x, y, z);
			}
			
		};
		
		//return new VoxelContextOlder(this, x, y, z);
	}*/

	/*@Override
	public void setVoxelData(Location location, int data)
	{
		setVoxelData((int) (double) location.x(), (int) (double) location.y(), (int) (double) location.z(), data);
	}

	@Override
	public void setVoxelData(int x, int y, int z, int data)
	{
		actuallySetsDataAt(x, y, z, data, null);
	}

	@Override
	public void setVoxelData(Location location, int data, Entity entity)
	{
		actuallySetsDataAt((int) (double) location.x(), (int) (double) location.y(), (int) (double) location.z(), data, entity);
	}

	@Override
	public void setVoxelData(int x, int y, int z, int data, Entity entity)
	{
		actuallySetsDataAt(x, y, z, data, entity);
	}*/

	/**
	 * Internal method that accesses the data
	 * 
	 * @return -1 if fails, the data of the new block if succeeds
	 */
	/*protected int actuallySetsDataAt(int x, int y, int z, int newData, Entity entity)
	{
		//TODO should VoxelModificationEvent be a part of the world implem poke() method ?
		
		x = sanitizeHorizontalCoordinate(x);
		y = sanitizeVerticalCoordinate(y);
		z = sanitizeHorizontalCoordinate(z);

		Chunk chunk = regions.getChunk(x / 32, y / 32, z / 32);
		
		if (chunk != null)
		{
			int formerData = chunk.getVoxelData(x % 32, y % 32, z % 32);
			Voxel formerVoxel = VoxelsStore.get().getVoxelById(formerData);
			Voxel newVoxel = VoxelsStore.get().getVoxelById(newData);

			try
			{
				//If we're merely changing the voxel meta 
				if (formerVoxel != null && newVoxel != null && formerVoxel.equals(newVoxel))
				{
					//Optionally runs whatever the voxel requires to run when modified
					if (formerVoxel instanceof VoxelLogic)
						newData = ((VoxelLogic) formerVoxel).onModification(this, x, y, z, formerData, newData, entity);
				}
				else
				{
					//Optionally runs whatever the voxel requires to run when removed
					if (formerVoxel instanceof VoxelLogic)
						((VoxelLogic) formerVoxel).onRemove(this, x, y, z, formerData, entity);

					//Optionally runs whatever the voxel requires to run when placed
					if (newVoxel instanceof VoxelLogic)
						newData = ((VoxelLogic) newVoxel).onPlace(this, x, y, z, newData, entity);
				}

			}
			//If it is stopped, don't try to go further
			catch (IllegalBlockModificationException illegal)
			{
				return -1;
			}

			chunk.setVoxelDataWithUpdates(x % 32, y % 32, z % 32, newData);
			
			getRegionsSummariesHolder().updateOnBlockPlaced(x, y, z, newData);

			//Neighbour chunks updates
			if (x % 32 == 0)
			{
				if (y % 32 == 0)
				{
					if (z % 32 == 0)
						regions.markChunkForReRender((x - 1) / 32, (y - 1) / 32, (z - 1) / 32);
					else if (z % 32 == 31)
						regions.markChunkForReRender((x - 1) / 32, (y - 1) / 32, (z + 1) / 32);
					regions.markChunkForReRender((x - 1) / 32, (y - 1) / 32, (z) / 32);
				}
				else if (y % 32 == 31)
				{
					if (z % 32 == 0)
						regions.markChunkForReRender((x - 1) / 32, (y + 1) / 32, (z - 1) / 32);
					else if (z % 32 == 31)
						regions.markChunkForReRender((x - 1) / 32, (y + 1) / 32, (z + 1) / 32);
					regions.markChunkForReRender((x - 1) / 32, (y + 1) / 32, (z) / 32);
				}
				else
				{
					if (z % 32 == 0)
						regions.markChunkForReRender((x - 1) / 32, (y) / 32, (z - 1) / 32);
					else if (z % 32 == 31)
						regions.markChunkForReRender((x - 1) / 32, (y) / 32, (z + 1) / 32);
					regions.markChunkForReRender((x - 1) / 32, (y) / 32, (z) / 32);
				}
			}
			else if (x % 32 == 31)
			{
				if (y % 32 == 0)
				{
					if (z % 32 == 0)
						regions.markChunkForReRender((x + 1) / 32, (y - 1) / 32, (z - 1) / 32);
					else if (z % 32 == 31)
						regions.markChunkForReRender((x + 1) / 32, (y - 1) / 32, (z + 1) / 32);
					regions.markChunkForReRender((x + 1) / 32, (y - 1) / 32, (z) / 32);
				}
				else if (y % 32 == 31)
				{
					if (z % 32 == 0)
						regions.markChunkForReRender((x + 1) / 32, (y + 1) / 32, (z - 1) / 32);
					else if (z % 32 == 31)
						regions.markChunkForReRender((x + 1) / 32, (y + 1) / 32, (z + 1) / 32);
					regions.markChunkForReRender((x + 1) / 32, (y + 1) / 32, (z) / 32);
				}
				else
				{
					if (z % 32 == 0)
						regions.markChunkForReRender((x + 1) / 32, (y) / 32, (z - 1) / 32);
					else if (z % 32 == 31)
						regions.markChunkForReRender((x + 1) / 32, (y) / 32, (z + 1) / 32);
					regions.markChunkForReRender((x + 1) / 32, (y) / 32, (z) / 32);
				}
			}
			if (y % 32 == 0)
			{
				if (z % 32 == 0)
					regions.markChunkForReRender((x) / 32, (y - 1) / 32, (z - 1) / 32);
				else if (z % 32 == 31)
					regions.markChunkForReRender((x) / 32, (y - 1) / 32, (z + 1) / 32);
				regions.markChunkForReRender((x) / 32, (y - 1) / 32, (z) / 32);
			}
			else if (y % 32 == 31)
			{
				if (z % 32 == 0)
					regions.markChunkForReRender((x) / 32, (y + 1) / 32, (z - 1) / 32);
				else if (z % 32 == 31)
					regions.markChunkForReRender((x) / 32, (y + 1) / 32, (z + 1) / 32);
				regions.markChunkForReRender((x) / 32, (y + 1) / 32, (z) / 32);
			}
			else
			{
				if (z % 32 == 0)
					regions.markChunkForReRender((x) / 32, (y) / 32, (z - 1) / 32);
				else if (z % 32 == 31)
					regions.markChunkForReRender((x) / 32, (y) / 32, (z + 1) / 32);
				regions.markChunkForReRender((x) / 32, (y) / 32, (z) / 32);
			}
			return newData;
		}
		else {
			//Chunk is null.REEE
			//throw new RuntimeException("Chunk wasn't loaded properly in fact :[ "+ x / 32 + " : " + y / 32 + " : " + z / 32);
		}
		return -1;
	}

	@Override
	public void setVoxelDataWithoutUpdates(int x, int y, int z, int i)
	{
		x = sanitizeHorizontalCoordinate(x);
		y = sanitizeVerticalCoordinate(y);
		z = sanitizeHorizontalCoordinate(z);

		//getRegionsSummariesHolder().updateOnBlockPlaced(x, y, z, i);

		Chunk c = regions.getChunk(x / 32, y / 32, z / 32);
		if (c != null)
		{
			c.setVoxelDataWithoutUpdates(x % 32, y % 32, z % 32, i);
		}
		else {
			//Chunk is null.REEE
			//throw new RuntimeException("Chunk wasn't loaded properly in fact :[ "+ x / 32 + " : " + y / 32 + " : " + z / 32);
		}
	}

	@Override
	public int getSunlightLevelWorldCoordinates(int x, int y, int z)
	{
		x = sanitizeHorizontalCoordinate(x);
		y = sanitizeVerticalCoordinate(y);
		z = sanitizeHorizontalCoordinate(z);
		if (this.isChunkLoaded(x / 32, y / 32, z / 32) && !this.getChunk(x / 32, y / 32, z / 32).isAirChunk())
			return VoxelFormat.sunlight(this.getVoxelData(x, y, z));
		else
			return y <= this.getRegionsSummariesHolder().getHeightAtWorldCoordinates(x, z) ? 0 : 15;
	}

	@Override
	public int getSunlightLevelLocation(Location location)
	{
		return getSunlightLevelWorldCoordinates((int) (double) location.x(), (int) (double) location.y(), (int) (double) location.z());
	}

	@Override
	public int getBlocklightLevelWorldCoordinates(int x, int y, int z)
	{
		x = sanitizeHorizontalCoordinate(x);
		y = sanitizeVerticalCoordinate(y);
		z = sanitizeHorizontalCoordinate(z);
		if (this.isChunkLoaded(x / 32, y / 32, z / 32))
			return VoxelFormat.blocklight(this.getVoxelData(x, y, z));
		else
			return 0;
	}

	@Override
	public int getBlocklightLevelLocation(Location location)
	{
		return getBlocklightLevelWorldCoordinates((int) (double) location.x(), (int) (double) location.y(), (int) (double) location.z());
	}*/
	
	@Override
	public IterableIterator<VoxelContext> getVoxelsWithin(CollisionBox boundingBox) {
		return new AABBVoxelIterator(this, boundingBox);
	}

	@Override
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

				c2.meshUpdater().requestMeshUpdate();
				//c2.markRenderInProgress(false);
				//c2.markForReRender();
			}
		}
	}

	@Override
	public Fence saveEverything()
	{
		CompoundFence regionsAndSummaries = new CompoundFence();
		
		//System.out.println("Saving all parts of world "+worldInfo.getName());
		regionsAndSummaries.add(regions.saveAll());
		regionsAndSummaries.add(getRegionsSummariesHolder().saveAllLoadedSummaries());

		this.worldInfo.save(new File(this.getFolderPath() + "/info.world"));
		this.internalData.setLong("entities-ids-counter", entitiesUUIDGenerator.get());
		this.internalData.setLong("worldTime", worldTime);
		this.internalData.setLong("worldTimeInternal", worldTicksCounter);
		this.internalData.setFloat("overcastFactor", overcastFactor);
		this.internalData.save();
		
		return regionsAndSummaries;
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

		VoxelContext peek;
		try {
			peek = this.peek(voxelLocation);
		} catch (WorldException e) {
			
			// Will not accept interacting with unloaded blocks
			return false;
		}
		
		Voxel voxel = peek.getVoxel();
		if (voxel != null && voxel instanceof VoxelInteractive)
			return ((VoxelInteractive) voxel).handleInteraction(entity, (ChunkVoxelContext) peek, input);
		return false;
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
	public Fence unloadUselessData()
	{
		Fence onlyThisHasAFence = this.getRegionsHolder().unloadsUselessData();
		//this.getRegionsSummariesHolder().unloadsUselessData();
		
		return onlyThisHasAFence;
		//return new TrivialFence();
	}

	private static final Logger logger = LoggerFactory.getLogger("world");
	public Logger logger() {
		return logger;
	}
}
