package io.xol.chunkstories.world;

import java.io.File;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.exceptions.IllegalBlockModificationException;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.particles.ParticleData;
import io.xol.chunkstories.api.particles.ParticleType;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelInteractive;
import io.xol.chunkstories.api.voxel.VoxelLogic;
import io.xol.chunkstories.api.world.Chunk;
import io.xol.chunkstories.api.world.ChunksIterator;
import io.xol.chunkstories.api.world.Region;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.WorldGenerator;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.api.world.WorldNetworked;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.FastConfig;
import io.xol.chunkstories.content.GameDirectory;
import io.xol.chunkstories.entity.EntityWorldIterator;
import io.xol.chunkstories.particles.ParticlesRenderer;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.WorldRenderer;
import io.xol.chunkstories.server.ServerPlayer;
import io.xol.chunkstories.tools.WorldTool;
import io.xol.chunkstories.voxel.VoxelTypes;
import io.xol.chunkstories.world.chunk.WorldChunksHolder;
import io.xol.chunkstories.world.chunk.ChunkRenderable;
import io.xol.chunkstories.world.io.IOTasks;
import io.xol.chunkstories.world.iterators.EntityRayIterator;
import io.xol.chunkstories.world.iterators.WorldChunksIterator;
import io.xol.chunkstories.world.summary.WorldHeightmapVersion;
import io.xol.engine.concurrency.SimpleLock;
import io.xol.engine.math.LoopingMathHelper;
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
	public long worldTime = 5000;
	float overcastFactor = 0.2f;

	//Who does the actual work
	public IOTasks ioHandler;

	// RAM-eating depreacated monster
	// public ChunksData chunksData;

	private WorldChunksHolder chunksHolder;

	// Heightmap management
	private WorldHeightmapVersion regionSummaries;

	// World-renderer backcall
	protected WorldRenderer renderer;

	// World logic thread
	private ScheduledExecutorService logic;

	// Temporary entity list
	private BlockingQueue<Entity> entities = new LinkedBlockingQueue<Entity>();
	//private ConcurrentHashMap<Long, Entity> localEntitiesByUUID = new ConcurrentHashMap<Long, Entity>();//new LinkedBlockingQueue<Entity>();
	public SimpleLock entitiesLock = new SimpleLock();

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
		this.chunksHolder = new WorldChunksHolder(this);
		this.regionSummaries = new WorldHeightmapVersion(this);
		this.logic = Executors.newSingleThreadScheduledExecutor();

		if (this instanceof WorldMaster)
		{
			this.folder = new File(GameDirectory.getGameFolderPath() + "/worlds/" + worldInfo.getInternalName());

			this.internalData = new ConfigFile(GameDirectory.getGameFolderPath() + "/worlds/" + worldInfo.getInternalName() + "/internal.dat");
			this.internalData.load();

			this.entitiesUUIDGenerator.set(internalData.getLongProp("entities-ids-counter", 0));
			this.worldTime = internalData.getLongProp("worldTime", 5000);
			this.overcastFactor = internalData.getFloatProp("overcastFactor", 0.2f);
		}
		else
		{
			this.folder = null;
			this.internalData = null;
		}
	}

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

	public void startLogic()
	{
		logic.scheduleAtFixedRate(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					tick();
				}
				catch (Exception e)
				{
					System.out.println("Son excellence le fils de pute de thread silencieusement suicidaire de mes couilles aurait un mot à dire: ");
					e.printStackTrace();
				}
			}
		}, 0, 16666, TimeUnit.MICROSECONDS);
	}

	public void stopLogic()
	{
		logic.shutdown();
	}

	@Override
	public void addEntity(final Entity entity)
	{
		//Assign an UUID to entities lacking one
		if (this instanceof WorldMaster && entity.getUUID() == -1)
		{
			long nextUUID = nextEntityId();
			entity.setUUID(nextUUID);
			System.out.println("Attributed UUID " + nextUUID + " to " + entity);
		}

		Entity check = this.getEntityByUUID(entity.getUUID());
		if (check != null)
		{
			System.out.println("Added an entity twice");
			Thread.dumpStack();
			System.exit(-1);
		}

		//Add it to the world
		entity.markHasSpawned();
		Location updatedLocation = entity.getLocation();
		updatedLocation.setWorld(this);
		entity.setLocation(updatedLocation);

		this.entities.add(entity);

		System.out.println("added " + entity + "to the worlde");
	}

	@Override
	public boolean removeEntity(Entity entity)
	{
		if (entity != null)
			return entity.removeFromWorld();

		return false;
	}

	@Override
	public boolean removeEntity(long uuid)
	{
		Entity entityFound = null;
		Iterator<Entity> iter = this.getAllLoadedEntities();
		while (iter.hasNext())
		{
			Entity next = iter.next();
			if (next.getUUID() == uuid)
			{
				entityFound = next;
				break;
				//iter.remove();
			}
		}

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
		return entities.remove(entity);
	}

	@Override
	public void tick()
	{
		if (this instanceof WorldNetworked)
		{
			//TODO net logic has nothing to do in world logic, it should be handled elsewere !!!
			//Deal with packets we received
			((WorldNetworked) this).processIncommingPackets();
		}

		entitiesLock.lock();
		try
		{
			Iterator<Entity> iter = this.getAllLoadedEntities();
			Entity entity;
			while (iter.hasNext())
			{
				//System.out.println("normal mv");
				entity = iter.next();
				if (entity instanceof EntityControllable && ((EntityControllable) entity).getControllerComponent().getController() != null && Client.controlledEntity != null && Client.controlledEntity.equals(entity))
				{
					//System.out.println("mdr");
					((EntityControllable) entity).tickClient(Client.getInstance());
				}

				Location entityLocation = entity.getLocation();
				if (entity.getChunkHolder() != null && entity.getChunkHolder().isDiskDataLoaded() && entity.getChunkHolder().isChunkLoaded((int) entityLocation.getX() / 32, (int) entityLocation.getY() / 32, (int) entityLocation.getZ() / 32))
				{
					if (entity instanceof EntityControllable)
					{
						Controller controller = ((EntityControllable) entity).getControllerComponent().getController();
						if (controller instanceof ServerPlayer)
						{
							//no
						}
						else if (controller instanceof Client)
							entity.tick();

					}
					else
						entity.tick();
				}

			}
			if (getParticlesHolder() != null)
				getParticlesHolder().updatePhysics();
			// worldTime++;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		entitiesLock.unlock();
	}

	@Override
	public Iterator<Entity> getAllLoadedEntities()
	{
		return new EntityWorldIterator(entities);
	}

	@Override
	public Entity getEntityByUUID(long entityID)
	{
		Iterator<Entity> ie = getAllLoadedEntities();
		Entity e;
		while (ie.hasNext())
		{
			e = ie.next();
			if (e.getUUID() == entityID)
				return e;
		}
		return null;
	}

	public int getMaxHeight()
	{
		return worldInfo.getSize().heightInChunks * 32;
	}

	public int getSizeInChunks()
	{
		return worldInfo.getSize().sizeInChunks;
	}

	@Override
	public double getWorldSize()
	{
		return getSizeInChunks() * 32d;
	}

	public Chunk getChunkWorldCoordinates(Location location, boolean load)
	{
		return getChunkWorldCoordinates((int) location.getX(), (int) location.getY(), (int) location.getZ(), load);
	}

	public Chunk getChunkWorldCoordinates(int worldX, int worldY, int worldZ, boolean load)
	{
		return getChunk(worldX / 32, worldY / 32, worldZ / 32, load);
	}

	public Chunk getChunk(int chunkX, int chunkY, int chunkZ, boolean load)
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
		return chunksHolder.getChunk(chunkX, chunkY, chunkZ, load);
	}

	public void removeChunk(Chunk c, boolean save)
	{
		removeChunk(c.getChunkX(), c.getChunkY(), c.getChunkZ(), save);
	}

	public void removeChunk(int chunkX, int chunkY, int chunkZ, boolean save)
	{
		chunkX = chunkX % getSizeInChunks();
		chunkZ = chunkZ % getSizeInChunks();
		if (chunkX < 0)
			chunkX += getSizeInChunks();
		if (chunkZ < 0)
			chunkZ += getSizeInChunks();
		if (chunkY < 0)
			chunkY = 0;
		//ioHandler.requestChunkUnload(chunkX, chunkY, chunkZ);
		chunksHolder.removeChunk(chunkX, chunkY, chunkZ, save);
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
		return this.chunksHolder.getChunk(chunkX, chunkY, chunkZ, false) != null;
	}

	public WorldHeightmapVersion getRegionSummaries()
	{
		return regionSummaries;
	}

	public int getVoxelData(Location location)
	{
		return getVoxelData((int) location.x, (int) location.y, (int) location.z);
	}

	public int getVoxelData(Location location, boolean load)
	{
		return getVoxelData((int) location.x, (int) location.y, (int) location.z, load);
	}

	public int getDataAt(Vector3d location, boolean load)
	{
		return getVoxelData((int) location.x, (int) location.y, (int) location.z, load);
	}

	public int getVoxelData(int x, int y, int z)
	{
		return getVoxelData(x, y, z, true);
	}

	@Override
	public int getVoxelData(int x, int y, int z, boolean load)
	{
		x = sanitizeHorizontalCoordinate(x);
		y = sanitizeVerticalCoordinate(y);
		z = sanitizeHorizontalCoordinate(z);

		Chunk c = chunksHolder.getChunk(x / 32, y / 32, z / 32, load);
		if (c != null)
			return c.getVoxelData(x, y, z);
		return 0;
	}

	@Override
	public void setVoxelData(int x, int y, int z, int data)
	{
		setVoxelData(x, y, z, data, true);
	}

	@Override
	public void setVoxelData(Location location, int data)
	{
		setVoxelData((int) location.x, (int) location.y, (int) location.z, data, true);
	}

	@Override
	public void setVoxelData(Location location, int data, boolean load)
	{
		setVoxelData((int) location.x, (int) location.y, (int) location.z, data, load);
	}

	@Override
	public void setVoxelData(int x, int y, int z, int data, boolean load)
	{
		actuallySetsDataAt(x, y, z, data, load, null);
	}

	@Override
	public void setVoxelData(Location location, int data, Entity entity)
	{
		actuallySetsDataAt((int) location.x, (int) location.y, (int) location.z, data, false, entity);
	}

	@Override
	public void setVoxelData(int x, int y, int z, int data, Entity entity)
	{
		actuallySetsDataAt(x, y, z, data, false, entity);
	}

	/**
	 * Internal method that accesses the data
	 * 
	 * @return -1 if fails, the data of the new block if succeeds
	 */
	protected int actuallySetsDataAt(int x, int y, int z, int newData, boolean load, Entity entity)
	{
		x = sanitizeHorizontalCoordinate(x);
		y = sanitizeVerticalCoordinate(y);
		z = sanitizeHorizontalCoordinate(z);

		getRegionSummaries().blockPlaced(x, y, z, newData);

		Chunk c = chunksHolder.getChunk(x / 32, y / 32, z / 32, load);
		if (c != null)
		{
			int formerData = c.getVoxelData(x % 32, y % 32, z % 32);
			Voxel formerVoxel = VoxelTypes.get(formerData);
			Voxel newVoxel = VoxelTypes.get(newData);

			try
			{
				//If we're merely changing the voxel meta 
				if(formerVoxel != null && newVoxel != null && formerVoxel.equals(newVoxel))
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

			c.setVoxelDataWithUpdates(x % 32, y % 32, z % 32, newData);

			//Neighbour chunks updates
			if (x % 32 == 0)
			{
				if (y % 32 == 0)
				{
					if (z % 32 == 0)
						chunksHolder.markChunkForReRender((x - 1) / 32, (y - 1) / 32, (z - 1) / 32);
					else if (z % 32 == 31)
						chunksHolder.markChunkForReRender((x - 1) / 32, (y - 1) / 32, (z + 1) / 32);
					chunksHolder.markChunkForReRender((x - 1) / 32, (y - 1) / 32, (z) / 32);
				}
				else if (y % 32 == 31)
				{
					if (z % 32 == 0)
						chunksHolder.markChunkForReRender((x - 1) / 32, (y + 1) / 32, (z - 1) / 32);
					else if (z % 32 == 31)
						chunksHolder.markChunkForReRender((x - 1) / 32, (y + 1) / 32, (z + 1) / 32);
					chunksHolder.markChunkForReRender((x - 1) / 32, (y + 1) / 32, (z) / 32);
				}
				else
				{
					if (z % 32 == 0)
						chunksHolder.markChunkForReRender((x - 1) / 32, (y) / 32, (z - 1) / 32);
					else if (z % 32 == 31)
						chunksHolder.markChunkForReRender((x - 1) / 32, (y) / 32, (z + 1) / 32);
					chunksHolder.markChunkForReRender((x - 1) / 32, (y) / 32, (z) / 32);
				}
			}
			else if (x % 32 == 31)
			{
				if (y % 32 == 0)
				{
					if (z % 32 == 0)
						chunksHolder.markChunkForReRender((x + 1) / 32, (y - 1) / 32, (z - 1) / 32);
					else if (z % 32 == 31)
						chunksHolder.markChunkForReRender((x + 1) / 32, (y - 1) / 32, (z + 1) / 32);
					chunksHolder.markChunkForReRender((x + 1) / 32, (y - 1) / 32, (z) / 32);
				}
				else if (y % 32 == 31)
				{
					if (z % 32 == 0)
						chunksHolder.markChunkForReRender((x + 1) / 32, (y + 1) / 32, (z - 1) / 32);
					else if (z % 32 == 31)
						chunksHolder.markChunkForReRender((x + 1) / 32, (y + 1) / 32, (z + 1) / 32);
					chunksHolder.markChunkForReRender((x + 1) / 32, (y + 1) / 32, (z) / 32);
				}
				else
				{
					if (z % 32 == 0)
						chunksHolder.markChunkForReRender((x + 1) / 32, (y) / 32, (z - 1) / 32);
					else if (z % 32 == 31)
						chunksHolder.markChunkForReRender((x + 1) / 32, (y) / 32, (z + 1) / 32);
					chunksHolder.markChunkForReRender((x + 1) / 32, (y) / 32, (z) / 32);
				}
			}
			if (y % 32 == 0)
			{
				if (z % 32 == 0)
					chunksHolder.markChunkForReRender((x) / 32, (y - 1) / 32, (z - 1) / 32);
				else if (z % 32 == 31)
					chunksHolder.markChunkForReRender((x) / 32, (y - 1) / 32, (z + 1) / 32);
				chunksHolder.markChunkForReRender((x) / 32, (y - 1) / 32, (z) / 32);
			}
			else if (y % 32 == 31)
			{
				if (z % 32 == 0)
					chunksHolder.markChunkForReRender((x) / 32, (y + 1) / 32, (z - 1) / 32);
				else if (z % 32 == 31)
					chunksHolder.markChunkForReRender((x) / 32, (y + 1) / 32, (z + 1) / 32);
				chunksHolder.markChunkForReRender((x) / 32, (y + 1) / 32, (z) / 32);
			}
			else
			{
				if (z % 32 == 0)
					chunksHolder.markChunkForReRender((x) / 32, (y) / 32, (z - 1) / 32);
				else if (z % 32 == 31)
					chunksHolder.markChunkForReRender((x) / 32, (y) / 32, (z + 1) / 32);
				chunksHolder.markChunkForReRender((x) / 32, (y) / 32, (z) / 32);
			}
			return newData;
		}
		return -1;
	}

	public void setVoxelDataWithoutUpdates(int x, int y, int z, int i, boolean load)
	{
		x = sanitizeHorizontalCoordinate(x);
		y = sanitizeVerticalCoordinate(y);
		z = sanitizeHorizontalCoordinate(z);

		getRegionSummaries().blockPlaced(x, y, z, i);

		Chunk c = chunksHolder.getChunk(x / 32, y / 32, z / 32, load);
		if (c != null)
		{
			c.setVoxelDataWithoutUpdates(x % 32, y % 32, z % 32, i);
		}
	}

	@Override
	public int getSunlightLevel(int x, int y, int z)
	{
		x = sanitizeHorizontalCoordinate(x);
		y = sanitizeVerticalCoordinate(y);
		z = sanitizeHorizontalCoordinate(z);
		if (this.isChunkLoaded(x / 32, y / 32, z / 32) && !this.getChunk(x / 32, y / 32, z / 32, false).isAirChunk())
			return VoxelFormat.sunlight(this.getVoxelData(x, y, z));
		else
			return y <= this.getRegionSummaries().getHeightAtWorldCoordinates(x, z) ? 0 : 15;
	}

	@Override
	public int getSunlightLevel(Location location)
	{
		return getSunlightLevel((int) location.x, (int) location.y, (int) location.z);
	}

	@Override
	public int getBlocklightLevel(int x, int y, int z)
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
	public int getBlocklightLevel(Location location)
	{
		return getBlocklightLevel((int) location.x, (int) location.y, (int) location.z);
	}

	public void setChunk(Chunk chunk)
	{
		if (this.isChunkLoaded(chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ()))
		{
			Chunk oldchunk = this.getChunk(chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ(), false);
			//if (oldchunk.dataPointer != chunk.dataPointer)
			oldchunk.destroy();

			System.out.println("Removed chunk " + chunk.toString());
		}
		chunksHolder.setChunk(chunk);
		if (renderer != null)
			renderer.flagModified();
	}

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

	public void unloadEverything()
	{
		chunksHolder.clearAll();
		getRegionSummaries().clearAll();
	}

	public void saveEverything()
	{
		System.out.println("Saving world");
		chunksHolder.saveAll();
		getRegionSummaries().saveAll();

		this.worldInfo.save(new File(this.getFolderPath() + "/info.txt"));
		this.internalData.setProp("entities-ids-counter", entitiesUUIDGenerator.get());
		this.internalData.setProp("worldTime", worldTime);
		this.internalData.setProp("overcastFactor", overcastFactor);
		this.internalData.save();
	}

	public void destroy()
	{
		//this.chunksData.destroy();
		this.chunksHolder.destroy();
		this.getRegionSummaries().destroy();
		this.logic.shutdown();
		if (this instanceof WorldMaster)
		{
			this.internalData.setProp("entities-ids-counter", entitiesUUIDGenerator.get());
			this.internalData.save();
		}
		ioHandler.kill();
	}

	public ChunksIterator getAllLoadedChunks()
	{
		return new WorldChunksIterator(this);
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

			Voxel v = VoxelTypes.get(id);
			/*CollisionBox[] boxes = v.getCollisionBoxes(data);
			if (boxes != null)
				for (CollisionBox box : boxes)
					if (box.isPointInside(posX, posY, posZ))
						return true;*/

			if (v.isVoxelSolid())
				return true;

		}
		return false;
	}

	public void trimRemovableChunks()
	{
		if (this instanceof WorldTool)
			System.out.println("omg this should not happen");

		if (Client.controlledEntity == null)
			return;
		Location loc = Client.controlledEntity.getLocation();
		ChunksIterator it = this.getAllLoadedChunks();
		Chunk chunk;
		while (it.hasNext())
		{
			chunk = it.next();
			if (chunk == null)
			{
				it.remove();
				continue;
			}
			boolean keep = false;
			//Iterate over possible things that hold chunks in memory
			if (!keep && Client.controlledEntity != null)
			{
				keep = true;
				int sizeInChunks = this.getSizeInChunks();
				int chunksViewDistance = (int) (FastConfig.viewDistance / 32) + 1;
				int pCX = (int) Math.floor(loc.x / 32);
				int pCY = (int) Math.floor(loc.y / 32);
				int pCZ = (int) Math.floor(loc.z / 32);

				//System.out.println("chunkX:"+chunk.chunkX+":"+chunk.chunkY+":"+chunk.chunkZ);

				if (((LoopingMathHelper.moduloDistance(chunk.getChunkX(), pCX, sizeInChunks) > chunksViewDistance) || (LoopingMathHelper.moduloDistance(chunk.getChunkZ(), pCZ, sizeInChunks) > chunksViewDistance)
						|| Math.abs(chunk.getChunkY() - pCY) > 3 + 1))
				{
					chunk.destroy();
					keep = false;
				}
			}
			//System.out.println(z);
			if (!keep)
				it.remove();
		}
	}

	public float getWeather()
	{
		return overcastFactor;
	}

	public long nextEntityId()
	{
		return entitiesUUIDGenerator.getAndIncrement();
	}

	public void setWeather(float overcastFactor)
	{
		this.overcastFactor = overcastFactor;
	}

	@Override
	public Location getDefaultSpawnLocation()
	{
		double dx = internalData.getDoubleProp("defaultSpawnX", 0.0);
		double dy = internalData.getDoubleProp("defaultSpawnY", 100.0);
		double dz = internalData.getDoubleProp("defaultSpawnZ", 0.0);
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
		Voxel voxel = VoxelTypes.get(dataAtLocation);
		if (voxel != null && voxel instanceof VoxelInteractive)
			return ((VoxelInteractive) voxel).handleInteraction(entity, voxelLocation, input, dataAtLocation);
		return false;
	}

	public Location raytraceSolid(Vector3d initialPosition, Vector3d direction, double limit)
	{
		return raytraceSolid(initialPosition, direction, limit, false, false);
	}

	public Location raytraceSolidOuter(Vector3d initialPosition, Vector3d direction, double limit)
	{
		return raytraceSolid(initialPosition, direction, limit, true, false);
	}

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
		x = (int) Math.floor(initialPosition.x);
		y = (int) Math.floor(initialPosition.y);
		z = (int) Math.floor(initialPosition.z);

		//DDA algorithm

		//It requires double arrays because it works using loops over each dimension
		double[] rayOrigin = new double[3];
		double[] rayDirection = new double[3];
		rayOrigin[0] = initialPosition.x;
		rayOrigin[1] = initialPosition.y;
		rayOrigin[2] = initialPosition.z;
		rayDirection[0] = direction.x;
		rayDirection[1] = direction.y;
		rayDirection[2] = direction.z;
		int voxelCoords[] = new int[] { x, y, z };
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
			vox = VoxelTypes.get(this.getVoxelData(x, y, z));
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

			distance += 1;
		}
		while (distance < limit);
		return null;
	}

	public Iterator<Entity> rayTraceEntities(Vector3d initialPosition, Vector3d direction, double limit)
	{
		double blocksLimit = limit;

		Vector3d blocksCollision = this.raytraceSolid(initialPosition, direction, limit);
		if (blocksCollision != null)
			blocksLimit = blocksCollision.distanceTo(initialPosition);

		return raytraceEntitiesIgnoringVoxels(initialPosition, direction, blocksLimit);
	}

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

	public ParticlesRenderer getParticlesHolder()
	{
		return particlesHolder;
	}

	public void setParticlesHolder(ParticlesRenderer particlesHolder)
	{
		this.particlesHolder = particlesHolder;
	}

	public ParticleData addParticle(ParticleType particleType, ParticleData data)
	{
		return particlesHolder.addParticle(particleType, data);
	}

	public ParticleData addParticle(ParticleType particleType, Vector3d position)
	{
		return particlesHolder.addParticle(particleType, new Location(this, position));
	}

	public void playSoundEffect(String soundEffect, Location location, float pitch, float gain)
	{
		if (this instanceof WorldClient)
		{
			Client.getInstance().getSoundManager().playSoundEffect(soundEffect, location, pitch, gain);
		}
	}

	public WorldChunksHolder getChunksHolder()
	{
		return chunksHolder;
	}

	public Region getRegionWorldCoordinates(Location location)
	{
		return getRegionWorldCoordinates((int) location.getX(), (int) location.getY(), (int) location.getZ());
	}

	public Region getRegionWorldCoordinates(int worldX, int worldY, int worldZ)
	{
		worldX = sanitizeHorizontalCoordinate(worldX);
		worldY = sanitizeVerticalCoordinate(worldY);
		worldZ = sanitizeHorizontalCoordinate(worldZ);

		return getRegion(worldX / 256, worldY / 256, worldZ / 256);
	}

	public Region getRegionChunkCoordinates(int chunkX, int chunkY, int chunkZ)
	{
		return getRegion(chunkX / 8, chunkY / 8, chunkZ / 8);
	}

	public Region getRegion(int regionX, int regionY, int regionZ)
	{
		return chunksHolder.getChunkHolderRegionCoordinates(regionX, regionY, regionZ, true);
	}

	public long getTime()
	{
		return worldTime;
	}
}
