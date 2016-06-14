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
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.world.Chunk;
import io.xol.chunkstories.api.world.ChunksIterator;
import io.xol.chunkstories.api.world.WorldGenerator;
import io.xol.chunkstories.api.world.WorldInterface;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.FastConfig;
import io.xol.chunkstories.content.GameDirectory;
import io.xol.chunkstories.entity.EntityImplementation;
import io.xol.chunkstories.entity.EntityIterator;
import io.xol.chunkstories.net.packets.PacketsProcessor.PendingSynchPacket;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.physics.particules.ParticlesHolder;
import io.xol.chunkstories.renderer.WorldRenderer;
import io.xol.chunkstories.server.ServerPlayer;
import io.xol.chunkstories.tools.WorldTool;
import io.xol.chunkstories.voxel.VoxelTypes;
import io.xol.chunkstories.world.WorldInfo.WorldSize;
import io.xol.chunkstories.world.chunk.ChunkHolder;
import io.xol.chunkstories.world.chunk.ChunksData;
import io.xol.chunkstories.world.chunk.ChunksHolders;
import io.xol.chunkstories.world.chunk.CubicChunk;
import io.xol.chunkstories.world.generator.WorldGenerators;
import io.xol.chunkstories.world.io.IOTasks;
import io.xol.chunkstories.world.iterators.WorldChunksIterator;
import io.xol.chunkstories.world.summary.RegionSummaries;
import io.xol.engine.concurrency.SimpleLock;
import io.xol.engine.math.LoopingMathHelper;
import io.xol.engine.math.lalgb.Vector3d;
import io.xol.engine.misc.ConfigFile;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public abstract class World implements WorldInterface
{
	protected WorldInfo worldInfo;
	private File folder;

	protected boolean client;
	private ConfigFile internalData;
	
	private WorldGenerator generator;

	// The world age, also tick counter. Can count for billions of real-world
	// time so we are not in trouble.
	// Let's say that the game world runs at 60Ticks per second
	public long worldTime = 5000;

	//Who does the actual work
	public IOTasks ioHandler;

	// RAM-eating depreacated monster
	public ChunksData chunksData;
	
	private ChunksHolders chunksHolder;

	// Heightmap management
	private RegionSummaries regionSummaries;

	// World-renderer backcall
	private WorldRenderer renderer;

	// World logic thread
	private ScheduledExecutorService logic;

	// Temporary entity list
	private BlockingQueue<Entity> entities = new LinkedBlockingQueue<Entity>();
	public SimpleLock entitiesLock = new SimpleLock();

	// Particles
	private ParticlesHolder particlesHolder;
	
	//Entity IDS counter
	AtomicLong veryLong = new AtomicLong();
	
	//Ugly primitive crap
	boolean raining;
	
	public World(WorldInfo info)
	{
		worldInfo = info;
		//Creates world generator
		this.generator = worldInfo.getGenerator();

		//Calls actual constructor
		generalInitialization();
	}

	public World(String name, String seed, WorldGenerator generator, WorldSize size)
	{
		//Builds a new WorldInfo
		worldInfo = new WorldInfo();
		worldInfo.setName(name);
		worldInfo.setSeed(seed);
		worldInfo.setSize(size);

		//Gets generator name
		worldInfo.setGeneratorName(WorldGenerators.getWorldGeneratorName(generator));
		this.generator = generator;
	
		//Calls actual constructor
		generalInitialization();
	}
	
	private void generalInitialization()
	{
		generator.initialize(this);
		
		chunksData = new ChunksData();
		setChunksHolder(new ChunksHolders(this, chunksData));
		setRegionSummaries(new RegionSummaries(this));
		logic = Executors.newSingleThreadScheduledExecutor();
		folder = new File(GameDirectory.getGameFolderPath() + "/worlds/" + worldInfo.getInternalName());

		internalData = new ConfigFile(GameDirectory.getGameFolderPath() + "/worlds/" + worldInfo.getInternalName() + "/internal.dat");
		internalData.load();
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
	 * @return
	 */
	public String getFolderPath()
	{
		return folder.getAbsolutePath();
	}

	public void linkWorldRenderer(WorldRenderer renderer)
	{
		this.renderer = renderer;
		setParticlesHolder(new ParticlesHolder());
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

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#addEntity(io.xol.chunkstories.api.entity.Entity)
	 */
	@Override
	public void addEntity(final Entity entity)
	{
		//EntityImplementation impl = (EntityImplementation) entity;
		if (this instanceof WorldMaster)
		{
			long nextUUID = nextEntityId();
			entity.setUUID(nextUUID);
			System.out.println("given "+nextUUID+" to "+entity);
		}
		Location currLocation = entity.getLocation();
		entity.setLocation(new Location(this, currLocation.getX(), currLocation.getY(), currLocation.getZ()));
		//entity.updatePosition();
		this.entities.add(entity);
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#removeEntity(io.xol.chunkstories.api.entity.Entity)
	 */
	@Override
	public void removeEntity(Entity entity)
	{
		Iterator<Entity> iter = this.getAllLoadedEntities();
		Entity entity2;
		while (iter.hasNext())
		{
			entity2 = iter.next();
			if (entity2.equals(entity))
			{
				entity.delete();
				iter.remove();
				//System.out.println("entity effectivly removed");
			}
		}
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#tick()
	 */
	@Override
	public void tick()
	{
		if(this instanceof WorldNetworked)
		{
			//TODO net logic has nothing to do in world logic, it should be handled elsewere !!!
			//Deal with packets we received
			((WorldNetworked)this).processIncommingPackets();
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
				if (entity instanceof EntityControllable && ((EntityControllable) entity).getControllerComponent().getController() != null 
						&& Client.controlledEntity != null && Client.controlledEntity.equals(entity))
				{
					//System.out.println("mdr");
					((EntityControllable) entity).tick(Client.getInstance());
				}
					
				//if (entity.getChunkHolder() != null && entity.getChunkHolder().isLoaded())
				if(entity instanceof EntityControllable)
				{
					Controller controller = ((EntityControllable) entity).getControllerComponent().getController();
					if(controller instanceof ServerPlayer)
					{
						//no
					}
					else if(controller instanceof Client)
						entity.tick();
						
				}
				else
					entity.tick();
				
				
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

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#getAllLoadedEntities()
	 */
	@Override
	public Iterator<Entity> getAllLoadedEntities()
	{
		return new EntityIterator(entities);//entities.iterator();
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#getEntityByUUID(long)
	 */
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

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#getMaxHeight()
	 */
	@Override
	public int getMaxHeight()
	{
		return worldInfo.getSize().height * 32;
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#getSizeInChunks()
	 */
	@Override
	public int getSizeInChunks()
	{
		return worldInfo.getSize().sizeInChunks;
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#getSizeSide()
	 */
	@Override
	public double getWorldSize()
	{
		return getSizeInChunks() * 32d;
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#getChunk(int, int, int, boolean)
	 */
	@Override
	public CubicChunk getChunk(int chunkX, int chunkY, int chunkZ, boolean load)
	{
		chunkX = chunkX % getSizeInChunks();
		chunkZ = chunkZ % getSizeInChunks();
		if (chunkX < 0)
			chunkX += getSizeInChunks();
		if (chunkZ < 0)
			chunkZ += getSizeInChunks();
		if (chunkY < 0)
			return null;
		if (chunkY >= worldInfo.getSize().height)
			return null;
		return getChunksHolder().getChunk(chunkX, chunkY, chunkZ, load);
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#removeChunk(io.xol.chunkstories.world.chunk.CubicChunk, boolean)
	 */
	@Override
	public void removeChunk(CubicChunk c, boolean save)
	{
		removeChunk(c.chunkX, c.chunkY, c.chunkZ, save);
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#removeChunk(int, int, int, boolean)
	 */
	@Override
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
		getChunksHolder().removeChunk(chunkX, chunkY, chunkZ, save);
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#isChunkLoaded(int, int, int)
	 */
	@Override
	public boolean isChunkLoaded(int chunkX, int chunkY, int chunkZ)
	{
		chunkX = chunkX % getSizeInChunks();
		chunkZ = chunkZ % getSizeInChunks();
		if (chunkX < 0)
			chunkX += getSizeInChunks();
		if (chunkZ < 0)
			chunkZ += getSizeInChunks();
		if (chunkY < 0)
			return false;
		if (chunkY >= worldInfo.getSize().height)
			return false;
		ChunkHolder h = getChunksHolder().getChunkHolder(chunkX, chunkY, chunkZ, false);
		if (h == null)
			return false;
		else
			return h.isChunkLoaded(chunkX, chunkY, chunkZ);
	}
	
	public RegionSummaries getRegionSummaries()
	{
		return regionSummaries;
	}

	public void setRegionSummaries(RegionSummaries regionSummaries)
	{
		this.regionSummaries = regionSummaries;
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#getDataAt(io.xol.chunkstories.api.Location)
	 */
	@Override
	public int getDataAt(Location location)
	{
		return getDataAt((int)location.x, (int)location.y, (int)location.z);
	}
	
	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#getDataAt(io.xol.chunkstories.api.Location, boolean)
	 */
	@Override
	public int getDataAt(Location location, boolean load)
	{
		return getDataAt(location, load);
	}
	
	public int getDataAt(Vector3d location, boolean load)
	{
		return getDataAt((int)location.x, (int)location.y, (int)location.z, load);
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#getDataAt(int, int, int)
	 */
	@Override
	public int getDataAt(int x, int y, int z)
	{
		return getDataAt(x, y, z, true);
	}
	
	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#getDataAt(int, int, int, boolean)
	 */
	@Override
	public int getDataAt(int x, int y, int z, boolean load)
	{
		x = sanitizeHorizontalCoordinate(x);
		y = sanitizeVerticalCoordinate(y);
		z = sanitizeHorizontalCoordinate(z);
		/*x = x % (getSizeInChunks() * 32);
		z = z % (getSizeInChunks() * 32);
		if (y < 0)
			y = 0;
		if (y > worldInfo.getSize().height * 32)
			y = worldInfo.getSize().height * 32;
		if (x < 0)
			x += getSizeInChunks() * 32;
		if (z < 0)
			z += getSizeInChunks() * 32;*/
		Chunk c = getChunksHolder().getChunk(x / 32, y / 32, z / 32, load);
		if (c != null)
			return c.getDataAt(x, y, z);
		return 0;
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#setDataAt(int, int, int, int)
	 */
	@Override
	public void setDataAt(int x, int y, int z, int i)
	{
		setDataAt(x, y, z, i, true);
	}
	
	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#setDataAt(io.xol.chunkstories.api.Location, int)
	 */
	@Override
	public void setDataAt(Location location, int i)
	{
		setDataAt((int)location.x, (int)location.y, (int)location.z, i, true);
	}
	
	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#setDataAt(io.xol.chunkstories.api.Location, int, boolean)
	 */
	@Override
	public void setDataAt(Location location, int i, boolean load)
	{
		setDataAt((int)location.x, (int)location.y, (int)location.z, i, load);
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#setDataAt(int, int, int, int, boolean)
	 */
	@Override
	public void setDataAt(int x, int y, int z, int i, boolean load)
	{
		x = sanitizeHorizontalCoordinate(x);
		y = sanitizeVerticalCoordinate(y);
		z = sanitizeHorizontalCoordinate(z);
		
		getRegionSummaries().blockPlaced(x, y, z, i);

		/*x = x % (getSizeInChunks() * 32);
		z = z % (getSizeInChunks() * 32);
		if (y < 0)
			y = 0;
		if (y > worldInfo.getSize().height * 32)
			y = worldInfo.getSize().height * 32;
		if (x < 0)
			x += getSizeInChunks() * 32;
		if (z < 0)
			z += getSizeInChunks() * 32;*/
		Chunk c = getChunksHolder().getChunk(x / 32, y / 32, z / 32, load);
		if (c != null)
		{
			synchronized (c)
			{
				c.setDataAtWithUpdates(x % 32, y % 32, z % 32, i);
				c.markDirty(true);
			}
			//Neighbour chunks updates
			if (x % 32 == 0)
			{
				if (y % 32 == 0)
				{
					if (z % 32 == 0)
						getChunksHolder().markChunkDirty((x - 1) / 32, (y - 1) / 32, (z - 1) / 32);
					else if (z % 32 == 31)
						getChunksHolder().markChunkDirty((x - 1) / 32, (y - 1) / 32, (z + 1) / 32);
					getChunksHolder().markChunkDirty((x - 1) / 32, (y - 1) / 32, (z) / 32);
				}
				else if (y % 32 == 31)
				{
					if (z % 32 == 0)
						getChunksHolder().markChunkDirty((x - 1) / 32, (y + 1) / 32, (z - 1) / 32);
					else if (z % 32 == 31)
						getChunksHolder().markChunkDirty((x - 1) / 32, (y + 1) / 32, (z + 1) / 32);
					getChunksHolder().markChunkDirty((x - 1) / 32, (y + 1) / 32, (z) / 32);
				}
				else
				{
					if (z % 32 == 0)
						getChunksHolder().markChunkDirty((x - 1) / 32, (y) / 32, (z - 1) / 32);
					else if (z % 32 == 31)
						getChunksHolder().markChunkDirty((x - 1) / 32, (y) / 32, (z + 1) / 32);
					getChunksHolder().markChunkDirty((x - 1) / 32, (y) / 32, (z) / 32);
				}
			}
			else if (x % 32 == 31)
			{
				if (y % 32 == 0)
				{
					if (z % 32 == 0)
						getChunksHolder().markChunkDirty((x + 1) / 32, (y - 1) / 32, (z - 1) / 32);
					else if (z % 32 == 31)
						getChunksHolder().markChunkDirty((x + 1) / 32, (y - 1) / 32, (z + 1) / 32);
					getChunksHolder().markChunkDirty((x + 1) / 32, (y - 1) / 32, (z) / 32);
				}
				else if (y % 32 == 31)
				{
					if (z % 32 == 0)
						getChunksHolder().markChunkDirty((x + 1) / 32, (y + 1) / 32, (z - 1) / 32);
					else if (z % 32 == 31)
						getChunksHolder().markChunkDirty((x + 1) / 32, (y + 1) / 32, (z + 1) / 32);
					getChunksHolder().markChunkDirty((x + 1) / 32, (y + 1) / 32, (z) / 32);
				}
				else
				{
					if (z % 32 == 0)
						getChunksHolder().markChunkDirty((x + 1) / 32, (y) / 32, (z - 1) / 32);
					else if (z % 32 == 31)
						getChunksHolder().markChunkDirty((x + 1) / 32, (y) / 32, (z + 1) / 32);
					getChunksHolder().markChunkDirty((x + 1) / 32, (y) / 32, (z) / 32);
				}
			}
			if (y % 32 == 0)
			{
				if (z % 32 == 0)
					getChunksHolder().markChunkDirty((x) / 32, (y - 1) / 32, (z - 1) / 32);
				else if (z % 32 == 31)
					getChunksHolder().markChunkDirty((x) / 32, (y - 1) / 32, (z + 1) / 32);
				getChunksHolder().markChunkDirty((x) / 32, (y - 1) / 32, (z) / 32);
			}
			else if (y % 32 == 31)
			{
				if (z % 32 == 0)
					getChunksHolder().markChunkDirty((x) / 32, (y + 1) / 32, (z - 1) / 32);
				else if (z % 32 == 31)
					getChunksHolder().markChunkDirty((x) / 32, (y + 1) / 32, (z + 1) / 32);
				getChunksHolder().markChunkDirty((x) / 32, (y + 1) / 32, (z) / 32);
			}
			else
			{
				if (z % 32 == 0)
					getChunksHolder().markChunkDirty((x) / 32, (y) / 32, (z - 1) / 32);
				else if (z % 32 == 31)
					getChunksHolder().markChunkDirty((x) / 32, (y) / 32, (z + 1) / 32);
				getChunksHolder().markChunkDirty((x) / 32, (y) / 32, (z) / 32);
			}
		}
	}

	public void setDataAtWithoutUpdates(int x, int y, int z, int i, boolean load)
	{
		x = sanitizeHorizontalCoordinate(x);
		y = sanitizeVerticalCoordinate(y);
		z = sanitizeHorizontalCoordinate(z);
		
		getRegionSummaries().blockPlaced(x, y, z, i);

		/*x = x % (getSizeInChunks() * 32);
		z = z % (getSizeInChunks() * 32);
		if (y < 0)
			y = 0;
		if (y > worldInfo.getSize().height * 32)
			y = worldInfo.getSize().height * 32;
		if (x < 0)
			x += getSizeInChunks() * 32;
		if (z < 0)
			z += getSizeInChunks() * 32;*/
		Chunk c = getChunksHolder().getChunk(x / 32, y / 32, z / 32, load);
		if (c != null)
		{
			synchronized (c)
			{
				c.setDataAtWithoutUpdates(x % 32, y % 32, z % 32, i);
			}
		}
	}

	@Override
	public int getSunlightLevel(int x, int y, int z)
	{
		x = sanitizeHorizontalCoordinate(x);
		y = sanitizeVerticalCoordinate(y);
		z = sanitizeHorizontalCoordinate(z);
		if(this.isChunkLoaded(x/32, y/32, z/32) && !this.getChunk(x/32, y/32, z/32, false).isAirChunk())
			return VoxelFormat.sunlight(this.getDataAt(x, y, z));
		else
			return y <= this.getRegionSummaries().getHeightAt(x, z) ? 0 : 15;
	}
	
	@Override
	public int getSunlightLevel(Location location)
	{
		return getSunlightLevel((int)location.x, (int)location.y, (int)location.z);
	}
		
	@Override
	public int getBlocklightLevel(int x, int y, int z)
	{
		x = sanitizeHorizontalCoordinate(x);
		y = sanitizeVerticalCoordinate(y);
		z = sanitizeHorizontalCoordinate(z);
		if(this.isChunkLoaded(x/32, y/32, z/32))
			return VoxelFormat.blocklight(this.getDataAt(x, y, z));
		else
			return 0;
	}
	
	@Override
	public int getBlocklightLevel(Location location)
	{
		return getBlocklightLevel((int)location.x, (int)location.y, (int)location.z);
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#setChunk(io.xol.chunkstories.world.chunk.CubicChunk)
	 */
	@Override
	public void setChunk(CubicChunk chunk)
	{
		if (this.isChunkLoaded(chunk.chunkX, chunk.chunkY, chunk.chunkZ))
		{
			CubicChunk oldchunk = this.getChunk(chunk.chunkX, chunk.chunkY, chunk.chunkZ, false);
			if (oldchunk.dataPointer != chunk.dataPointer)
				oldchunk.destroy();
			// System.out.println("Removed chunk "+chunk.toString());
		}
		getChunksHolder().setChunk(chunk);
		if (renderer != null)
			renderer.flagModified();
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#reRender()
	 */
	@Override
	public synchronized void redrawAllChunks()
	{
		ChunksIterator i = this.getAllLoadedChunks();
		CubicChunk c;
		while (i.hasNext())
		{
			c = i.next();
			c.need_render.set(true);
			c.requestable.set(true);
			if(c.chunkRenderData != null)
				c.chunkRenderData.markForDeletion();
			c.chunkRenderData = null;
		}
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#clear()
	 */
	@Override
	public void unloadEverything()
	{
		getChunksHolder().clearAll();
		getRegionSummaries().clearAll();
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#save()
	 */
	@Override
	public void saveEverything()
	{
		System.out.println("Saving world");
		getChunksHolder().saveAll();
		getRegionSummaries().saveAll();

		this.worldInfo.save(new File(this.getFolderPath()+"/info.txt"));
		this.internalData.setProp("entities-ids-counter", veryLong.get());
		this.internalData.save();
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#destroy()
	 */
	@Override
	public void destroy()
	{
		this.chunksData.destroy();
		this.getChunksHolder().destroy();
		this.getRegionSummaries().destroy();
		this.logic.shutdown();
		if (!client)
		{
			this.internalData.setProp("entities-ids-counter", veryLong.get());
			this.internalData.save();
		}
		ioHandler.kill();
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#iterator()
	 */
	@Override
	public ChunksIterator getAllLoadedChunks()
	{
		return new WorldChunksIterator(this);
	}

	/**
	 * Legacy crap for particle system
	 * @param posX
	 * @param posY
	 * @param posZ
	 * @return
	 */
	public boolean checkCollisionPoint(double posX, double posY, double posZ)
	{
		int data = this.getDataAt((int) posX, (int) posY, (int) posZ);
		int id = VoxelFormat.id(data);
		if (id > 0)
		{

			/*Voxel v = VoxelTypes.get(id);
			CollisionBox[] boxes = v.getCollisionBoxes(data);
			if (boxes != null)
				for (CollisionBox box : boxes)
					if (box.isPointInside(posX, posY, posZ))
						return true;
			*/
			return true;

		}
		return false;
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#trimRemovableChunks()
	 */
	@Override
	public void trimRemovableChunks()
	{
		if (this instanceof WorldTool)
			System.out.println("omg this should not happen");
		
		if(Client.controlledEntity == null)
			return;
		Location loc = Client.controlledEntity.getLocation();
		ChunksIterator it = this.getAllLoadedChunks();
		CubicChunk chunk;
		while (it.hasNext())
		{
			chunk = it.next();
			if (chunk == null)
			{
				it.remove();
				continue;
			}
			boolean keep = false;
			if (!keep && Client.controlledEntity != null)
			{
				keep = true;
				int sizeInChunks = this.getSizeInChunks();
				int chunksViewDistance = (int) (FastConfig.viewDistance / 32);
				int pCX = (int) loc.x / 32;
				int pCY = (int) loc.y / 32;
				int pCZ = (int) loc.z / 32;

				if (((LoopingMathHelper.moduloDistance(chunk.chunkX, pCX, sizeInChunks) > chunksViewDistance) || (LoopingMathHelper.moduloDistance(chunk.chunkZ, pCZ, sizeInChunks) > chunksViewDistance) || (chunk.chunkY - pCY) > 3 || (chunk.chunkY - pCY) < -3))
				{
					if(chunk.chunkRenderData != null)
						chunk.chunkRenderData.markForDeletion();
					chunk.need_render.set(true);
					keep = false;
				}
			}
			//System.out.println(z);
			if (!keep)
				it.remove();
		}
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#isRaining()
	 */
	@Override
	public boolean isRaining()
	{
		return raining;
	}

	public long nextEntityId()
	{
		return veryLong.getAndIncrement();
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#setWeather(boolean)
	 */
	@Override
	public void setWeather(boolean booleanProp)
	{
		raining = booleanProp;
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#getDefaultSpawnLocation()
	 */
	@Override
	public Location getDefaultSpawnLocation()
	{
		double dx = internalData.getDoubleProp("defaultSpawnX", 0.0);
		double dy = internalData.getDoubleProp("defaultSpawnY", 100.0);
		double dz = internalData.getDoubleProp("defaultSpawnZ", 0.0);
		return new Location(this, dx, dy, dz);
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#setTime(long)
	 */
	@Override
	public void setTime(long time)
	{
		this.worldTime = time;
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#getGenerator()
	 */
	@Override
	public WorldGenerator getGenerator()
	{
		return generator;
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#handleInteraction(io.xol.chunkstories.api.entity.Entity, io.xol.chunkstories.api.Location, io.xol.chunkstories.api.input.Input)
	 */
	@Override
	public boolean handleInteraction(Entity entity, Location blockLocation, Input input)
	{
		return false;
	}
	
	public Location raytraceSolid(Location initialPosition, Vector3d direction, double limit)
	{
		return raytraceSolid(initialPosition, direction, limit, false);
	}
	
	public Location raytraceSolidOuter(Location initialPosition, Vector3d direction, double limit)
	{
		return raytraceSolid(initialPosition, direction, limit, true);
	}
	
	private Location raytraceSolid(Location initialPosition, Vector3d direction, double limit, boolean outer)
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
			vox = VoxelTypes.get(this.getDataAt(x, y, z));
			if (vox.isVoxelSolid() || vox.isVoxelSelectable())
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
		if (coordinate > worldInfo.getSize().height * 32)
			coordinate = worldInfo.getSize().height * 32;
		return coordinate;
	}

	public ParticlesHolder getParticlesHolder()
	{
		return particlesHolder;
	}

	public void setParticlesHolder(ParticlesHolder particlesHolder)
	{
		this.particlesHolder = particlesHolder;
	}

	public ChunksHolders getChunksHolder()
	{
		return chunksHolder;
	}

	public void setChunksHolder(ChunksHolders chunksHolder)
	{
		this.chunksHolder = chunksHolder;
	}
}
