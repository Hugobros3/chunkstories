package io.xol.chunkstories.world;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.xol.chunkstories.api.GameLogic;
import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.entity.interfaces.EntityWithClientPrediction;
import io.xol.chunkstories.api.exceptions.IllegalBlockModificationException;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.particles.ParticlesManager;
import io.xol.chunkstories.api.utils.IterableIterator;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelInteractive;
import io.xol.chunkstories.api.voxel.VoxelLogic;
import io.xol.chunkstories.api.world.WorldGenerator;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.api.world.chunk.ChunkHolder;
import io.xol.chunkstories.api.world.chunk.WorldUser;
import io.xol.chunkstories.api.world.chunk.ChunksIterator;
import io.xol.chunkstories.api.world.chunk.Region;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.content.GameDirectory;
import io.xol.chunkstories.content.sandbox.GameLogicThread;
import io.xol.chunkstories.content.sandbox.UnthrustedUserContentSecurityManager;
import io.xol.chunkstories.entity.EntityWorldIterator;
import io.xol.chunkstories.particles.ParticlesRenderer;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.WorldRenderer;
import io.xol.chunkstories.renderer.chunks.ChunkRenderable;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.chunkstories.voxel.Voxels;
import io.xol.chunkstories.world.chunk.CubicChunk;
import io.xol.chunkstories.world.io.IOTasks;
import io.xol.chunkstories.world.iterators.EntityRayIterator;
import io.xol.chunkstories.world.iterators.WorldChunksIterator;
import io.xol.chunkstories.world.region.RegionImplementation;
import io.xol.chunkstories.world.region.WorldRegionsHolder;
import io.xol.chunkstories.world.summary.WorldRegionSummariesHolder;
import io.xol.engine.math.lalgb.Vector3d;
import io.xol.engine.misc.ConfigFile;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public abstract class WorldImplementation implements World
{
	protected WorldInfo worldInfo;
	private final File folder;

	//protected final boolean client;
	private final ConfigFile internalData;

	private WorldGenerator generator;

	// The world age, also tick counter. Can count for billions of real-world
	// time so we are not in trouble.
	// Let's say that the game world runs at 60Ticks per second
	public long worldTicksCounter = 0;
	
	//Timecycle counter
	public long worldTime = 5000;
	float overcastFactor = 0.2f;

	//Who does the actual work
	public IOTasks ioHandler;
	private GameLogicThread worldThread;

	// RAM-eating depreacated monster
	// public ChunksData chunksData;

	private WorldRegionsHolder regions;

	// Heightmap management
	private WorldRegionSummariesHolder regionSummaries;

	// World-renderer backcall
	protected WorldRenderer renderer;
	
	// Temporary entity list
	protected EntitiesHolder entities = new EntitiesHolder();
	
	public ReadWriteLock entitiesLock = new ReentrantReadWriteLock();

	// Particles
	private ParticlesRenderer particlesHolder;

	//Entity IDS counter
	AtomicLong entitiesUUIDGenerator = new AtomicLong();

	public WorldImplementation(WorldInfo info)
	{
		worldInfo = info;
		//Creates world generator
		this.generator = worldInfo.getGenerator();

		this.generator.initialize(this);

		//this.chunksData = new ChunksData();
		this.regions = new WorldRegionsHolder(this);
		this.regionSummaries = new WorldRegionSummariesHolder(this);
		//this.logic = Executors.newSingleThreadScheduledExecutor();

		if (this instanceof WorldMaster)
		{
			this.folder = new File(GameDirectory.getGameFolderPath() + "/worlds/" + worldInfo.getInternalName());

			this.internalData = new ConfigFile(GameDirectory.getGameFolderPath() + "/worlds/" + worldInfo.getInternalName() + "/internal.dat");
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
		
		//Start the world logic thread
		worldThread = new GameLogicThread(this, new UnthrustedUserContentSecurityManager());
	}

	public void startLogic()
	{
		worldThread.start();
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

	@Override
	public void addEntity(final Entity entity)
	{
		//Assign an UUID to entities lacking one
		if (this instanceof WorldMaster && entity.getUUID() == -1)
		{
			long nextUUID = nextEntityId();
			entity.setUUID(nextUUID);
			//System.out.println("Attributed UUID " + nextUUID + " to " + entity);
		}

		Entity check = this.getEntityByUUID(entity.getUUID());
		if (check != null)
		{
			ChunkStoriesLogger.getInstance().log("Added an entity twice");
			ChunkStoriesLogger.getInstance().save();
			Thread.dumpStack();
			System.exit(-1);
		}

		//Add it to the world
		entity.markHasSpawned();
		Location updatedLocation = entity.getLocation();
		updatedLocation.setWorld(this);
		entity.setLocation(updatedLocation);

		this.entities.insertEntity(entity);

		//System.out.println("added " + entity + "to the worlde");
	}

	@Override
	public boolean removeEntity(Entity entity)
	{
		if (entity != null)
			return entity.removeFromWorld();

		return false;
	}

	@Override
	public boolean removeEntityByUUID(long uuid)
	{
		Entity entityFound = this.getEntityByUUID(uuid);

		if (entityFound != null)
			return entityFound.removeFromWorld();

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
		//Place the entire tick() method in a try/catch
		try
		{
			//Iterates over every entity
			entitiesLock.writeLock().lock();
			Iterator<Entity> iter = this.getAllLoadedEntities();
			Entity entity;
			while (iter.hasNext())
			{
				entity = iter.next();

				//Check entity's region is loaded
				//Location entityLocation = entity.getLocation();
				if (entity.getRegion() != null && entity.getRegion().isDiskDataLoaded())// && entity.getChunkHolder().isChunkLoaded((int) entityLocation.getX() / 32, (int) entityLocation.getY() / 32, (int) entityLocation.getZ() / 32))
				{
					//If we're the client world and this is our controlled entity we execute the tickClientController() and tick() methods
					if (this instanceof WorldClient && entity instanceof EntityControllable && ((EntityControllable) entity).getControllerComponent().getController() != null && Client.getInstance().getClientSideController().getControlledEntity() != null && Client.getInstance().getClientSideController().getControlledEntity().equals(entity))
					{
						((EntityControllable) entity).tickClientController(Client.getInstance().getClientSideController());
						entity.tick();
					}
					else if(this instanceof WorldClient)
					{
						//Some entities feature fancy clientside-prediction, for misc functions such as interpolation positions, spawning particles or playing walking sounds
						if(entity instanceof EntityWithClientPrediction)
						{
							if(entity instanceof EntityControllable)
							{
								Controller controller = ((EntityControllable) entity).getControllerComponent().getController();
								//If this is a remote world, any non-controlled entity could be client-predicted
								if(this instanceof WorldClientRemote)
								{
									//Ok
								}
								//If this is a local world/server then only REMOTE clients should be predicted
								else if(controller != null && !controller.equals(Client.getInstance().getClientSideController()))
								{
									//Ok too
								}
								//If neither is true, abort
								else
									continue;
							}
							//Non-controllable, locally simulated entities should not be predicted
							else if(this instanceof WorldClientLocal)
								continue;
							
							((EntityWithClientPrediction) entity).tickClientPrediction();
						}
					}
					//Server should not tick client's entities, only ticks if their controller isn't present
					else if (this instanceof WorldMaster && (!(entity instanceof EntityControllable) || ((EntityControllable) entity).getControllerComponent().getController() == null))
						entity.tick();
				}
				//Tries to snap the entity to the region if it ends up being loaded
				else if(entity.getRegion() == null)
					entity.getEntityComponentPosition().trySnappingToRegion();

			}
			entitiesLock.writeLock().unlock();

			//Update particles subsystem if it exists
			if (getParticlesManager() != null && getParticlesManager() instanceof ParticlesRenderer)
				((ParticlesRenderer) getParticlesManager()).updatePhysics();

			//Increase the ticks counter
			worldTicksCounter++;
			
			//Time cycle
			if (this instanceof WorldMaster && internalData.getBoolean("doTimeCycle", true))
				if(worldTicksCounter % 60 == 0)
					worldTime++;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public IterableIterator<Entity> getAllLoadedEntities()
	{
		return new EntityWorldIterator(entities.iterator());
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
	
	@Override
	public int getVoxelData(Vector3d location)
	{
		return getVoxelData((int) location.getX(), (int) location.getY(), (int) location.getZ());
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
	}

	@Override
	public void setVoxelData(Location location, int data)
	{
		setVoxelData((int) location.getX(), (int) location.getY(), (int) location.getZ(), data);
	}

	@Override
	public void setVoxelData(int x, int y, int z, int data)
	{
		actuallySetsDataAt(x, y, z, data, null);
	}

	@Override
	public void setVoxelData(Location location, int data, Entity entity)
	{
		actuallySetsDataAt((int) location.getX(), (int) location.getY(), (int) location.getZ(), data, entity);
	}

	@Override
	public void setVoxelData(int x, int y, int z, int data, Entity entity)
	{
		actuallySetsDataAt(x, y, z, data, entity);
	}

	/**
	 * Internal method that accesses the data
	 * 
	 * @return -1 if fails, the data of the new block if succeeds
	 */
	protected int actuallySetsDataAt(int x, int y, int z, int newData, Entity entity)
	{
		x = sanitizeHorizontalCoordinate(x);
		y = sanitizeVerticalCoordinate(y);
		z = sanitizeHorizontalCoordinate(z);

		getRegionsSummariesHolder().updateOnBlockPlaced(x, y, z, newData);

		Chunk chunk = regions.getChunk(x / 32, y / 32, z / 32);
		if (chunk != null)
		{
			int formerData = chunk.getVoxelData(x % 32, y % 32, z % 32);
			Voxel formerVoxel = Voxels.get(formerData);
			Voxel newVoxel = Voxels.get(newData);

			try
			{
				//If we're merely changing the voxel meta 
				if (formerVoxel != null && newVoxel != null && formerVoxel.equals(newVoxel))
				{
					//Optionally runs whatever the voxel requires to run when modified
					if (formerVoxel instanceof VoxelLogic)
						newData = ((VoxelLogic) formerVoxel).onModification(this, x, y, z, newData, entity);
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
		return -1;
	}

	@Override
	public void setVoxelDataWithoutUpdates(int x, int y, int z, int i)
	{
		x = sanitizeHorizontalCoordinate(x);
		y = sanitizeVerticalCoordinate(y);
		z = sanitizeHorizontalCoordinate(z);

		getRegionsSummariesHolder().updateOnBlockPlaced(x, y, z, i);

		Chunk c = regions.getChunk(x / 32, y / 32, z / 32);
		if (c != null)
		{
			c.setVoxelDataWithoutUpdates(x % 32, y % 32, z % 32, i);
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
		return getSunlightLevelWorldCoordinates((int) location.getX(), (int) location.getY(), (int) location.getZ());
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
		return getBlocklightLevelWorldCoordinates((int) location.getX(), (int) location.getY(), (int) location.getZ());
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

				c2.markRenderInProgress(false);
				c2.destroyRenderData();
			}
		}
	}

	//@Override
	public void unloadEverything()
	{
		//TODO exterminate this
		regions.clearAll();
		//getRegionSummaries().clearAll();
	}

	@Override
	public void saveEverything()
	{
		//System.out.println("Saving all parts of world "+worldInfo.getName());
		regions.saveAll();
		getRegionsSummariesHolder().saveAllLoadedSummaries();

		this.worldInfo.save(new File(this.getFolderPath() + "/info.txt"));
		this.internalData.setLong("entities-ids-counter", entitiesUUIDGenerator.get());
		this.internalData.setLong("worldTime", worldTime);
		this.internalData.setLong("worldTimeInternal", worldTicksCounter);
		this.internalData.setFloat("overcastFactor", overcastFactor);
		this.internalData.save();
	}
	
	/**
	 * Legacy crap for particle system
	 */
	public boolean checkCollisionPoint(double posX, double posY, double posZ)
	{
		int data = this.getVoxelData((int) posX, (int) posY, (int) posZ);
		int id = VoxelFormat.id(data);
		if (id > 0)
		{

			Voxel v = Voxels.get(id);
			
			CollisionBox[] boxes = v.getTranslatedCollisionBoxes(this, (int) posX, (int) posY, (int) posZ);
			if (boxes != null)
				for (CollisionBox box : boxes)
					if (box.isPointInside(posX, posY, posZ))
						return true;

			if (v.isVoxelSolid())
				return true;

		}
		return false;
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

		int dataAtLocation = this.getVoxelData(voxelLocation);
		Voxel voxel = Voxels.get(dataAtLocation);
		if (voxel != null && voxel instanceof VoxelInteractive)
			return ((VoxelInteractive) voxel).handleInteraction(entity, voxelLocation, input, dataAtLocation);
		return false;
	}

	@Override
	public Location raytraceSolid(Vector3d initialPosition, Vector3d direction, double limit)
	{
		return raytraceSolid(initialPosition, direction, limit, false, false);
	}

	@Override
	public Location raytraceSolidOuter(Vector3d initialPosition, Vector3d direction, double limit)
	{
		return raytraceSolid(initialPosition, direction, limit, true, false);
	}

	@Override
	public Location raytraceSelectable(Location initialPosition, Vector3d direction, double limit)
	{
		return raytraceSolid(initialPosition, direction, limit, false, true);
	}

	private Location raytraceSolid(Vector3d initialPosition, Vector3d direction, double limit, boolean outer, boolean selectable)
	{
		direction.normalize();
		//direction.scale(0.02);

		float distance = 0f;
		Voxel vox;
		int x, y, z;
		x = (int) Math.floor(initialPosition.getX());
		y = (int) Math.floor(initialPosition.getY());
		z = (int) Math.floor(initialPosition.getZ());

		//DDA algorithm

		//It requires double arrays because it works using loops over each dimension
		double[] rayOrigin = new double[3];
		double[] rayDirection = new double[3];
		rayOrigin[0] = initialPosition.getX();
		rayOrigin[1] = initialPosition.getY();
		rayOrigin[2] = initialPosition.getZ();
		rayDirection[0] = direction.getX();
		rayDirection[1] = direction.getY();
		rayDirection[2] = direction.getZ();
		int voxelCoords[] = new int[] { x, y, z };
		int voxelDelta[] = new int[] { 0, 0, 0 };
		double[] deltaDist = new double[3];
		double[] next = new double[3];
		int step[] = new int[3];

		int side = 0;
		//Prepare distances
		for (int i = 0; i < 3; ++i)
		{
			double deltaX = rayDirection[0] / rayDirection[i];
			double deltaY = rayDirection[1] / rayDirection[i];
			double deltaZ = rayDirection[2] / rayDirection[i];
			deltaDist[i] = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
			if (rayDirection[i] < 0.f)
			{
				step[i] = -1;
				next[i] = (rayOrigin[i] - voxelCoords[i]) * deltaDist[i];
			}
			else
			{
				step[i] = 1;
				next[i] = (voxelCoords[i] + 1.f - rayOrigin[i]) * deltaDist[i];
			}
		}

		do
		{
			x = voxelCoords[0];
			y = voxelCoords[1];
			z = voxelCoords[2];
			vox = Voxels.get(this.getVoxelData(x, y, z));
			if (vox.isVoxelSolid() || (selectable && vox.isVoxelSelectable()))
			{
				boolean collides = false;
				for (CollisionBox box : vox.getTranslatedCollisionBoxes(this, x, y, z))
				{
					//System.out.println(box);
					Vector3d collisionPoint = box.collidesWith(initialPosition, direction);
					if (collisionPoint != null)
					{
						collides = true;
						//System.out.println("collides @ "+collisionPoint);
					}
				}
				if (collides)
				{
					if (!outer)
						return new Location(this, x, y, z);
					else
					{
						//Back off a bit
						switch (side)
						{
						case 0:
							x -= step[side];
							break;
						case 1:
							y -= step[side];
							break;
						case 2:
							z -= step[side];
							break;
						}
						return new Location(this, x, y, z);
					}
				}
			}
			//DDA steps
			side = 0;
			for (int i = 1; i < 3; ++i)
			{
				if (next[side] > next[i])
				{
					side = i;
				}
			}
			next[side] += deltaDist[side];
			voxelCoords[side] += step[side];
			voxelDelta[side] += step[side];

			//System.out.println(deltaDist[side]);
			distance += deltaDist[side];
			
			//System.out.println(Math.sqrt(voxelDelta[0] * voxelDelta[0] + voxelDelta[1] * voxelDelta[1] + voxelDelta[2] * voxelDelta[2])+ " < "+Math.sqrt(limit * limit));
		}
		while (voxelDelta[0] * voxelDelta[0] + voxelDelta[1] * voxelDelta[1] + voxelDelta[2] * voxelDelta[2] < limit * limit);
		return null;
	}

	@Override
	public Iterator<Entity> rayTraceEntities(Vector3d initialPosition, Vector3d direction, double limit)
	{
		double blocksLimit = limit;

		Vector3d blocksCollision = this.raytraceSolid(initialPosition, direction, limit);
		if (blocksCollision != null)
			blocksLimit = blocksCollision.distanceTo(initialPosition);

		return raytraceEntitiesIgnoringVoxels(initialPosition, direction, Math.min(blocksLimit, limit));
	}

	@Override
	public Iterator<Entity> raytraceEntitiesIgnoringVoxels(Vector3d initialPosition, Vector3d direction, double limit)
	{
		return new EntityRayIterator(this, initialPosition, direction, limit);
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
		if (coordinate > worldInfo.getSize().heightInChunks * 32)
			coordinate = worldInfo.getSize().heightInChunks * 32;
		return coordinate;
	}

	@Override
	public ParticlesManager getParticlesManager()
	{
		return particlesHolder;
	}

	public void setParticlesManager(ParticlesRenderer particlesHolder)
	{
		this.particlesHolder = particlesHolder;
	}

	@Override
	public ChunkHolder aquireChunkHolderLocation(WorldUser user, Location location)
	{
		return aquireChunkHolder(user, (int) location.getX(), (int) location.getY(), (int) location.getZ());
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
		return getChunkWorldCoordinates((int) location.getX(), (int) location.getY(), (int) location.getZ());
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
	
	public WorldRegionsHolder getRegionsHolder()
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
		return aquireRegionWorldCoordinates(user, (int) location.getX(), (int) location.getY(), (int) location.getZ());
	}
	
	@Override
	public RegionImplementation getRegionLocation(Location location)
	{
		return getRegionWorldCoordinates((int) location.getX(), (int) location.getY(), (int) location.getZ());
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
	public void destroy()
	{
		//Stop the game logic first
		worldThread.stopLogicThread();
		
		this.regions.destroy();
		this.getRegionsSummariesHolder().destroy();
		//this.logic.shutdown();
		if (this instanceof WorldMaster)
		{
			this.internalData.setLong("entities-ids-counter", entitiesUUIDGenerator.get());
			this.internalData.save();
		}
		
		//Kill the IO handler
		ioHandler.kill();
	}

	public void unloadUselessData()
	{
		this.getRegionsHolder().unloadsUselessData();
		this.getRegionsSummariesHolder().unloadsUselessData();
	}

}
