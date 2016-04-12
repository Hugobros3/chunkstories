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
import io.xol.chunkstories.api.entity.ClientController;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.world.Chunk;
import io.xol.chunkstories.api.world.ChunksIterator;
import io.xol.chunkstories.api.world.WorldGenerator;
import io.xol.chunkstories.api.world.WorldInterface;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.FastConfig;
import io.xol.chunkstories.content.GameDirectory;
import io.xol.chunkstories.entity.EntityControllable;
import io.xol.chunkstories.entity.EntityImpl;
import io.xol.chunkstories.entity.EntityIterator;
import io.xol.chunkstories.physics.particules.ParticlesHolder;
import io.xol.chunkstories.renderer.WorldRenderer;
import io.xol.chunkstories.tools.WorldTool;
import io.xol.chunkstories.world.WorldInfo.WorldSize;
import io.xol.chunkstories.world.chunk.ChunkHolder;
import io.xol.chunkstories.world.chunk.ChunksData;
import io.xol.chunkstories.world.chunk.ChunksHolders;
import io.xol.chunkstories.world.chunk.CubicChunk;
import io.xol.chunkstories.world.io.IOTasks;
import io.xol.chunkstories.world.iterators.WorldChunksIterator;
import io.xol.chunkstories.world.summary.RegionSummaries;
import io.xol.engine.math.LoopingMathHelper;
import io.xol.engine.misc.ConfigFile;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

/**
 * @author Hugo
 *
 */
public abstract class World implements WorldInterface
{
	public String name;
	public String seed;
	public File folder;

	public long worldTime = 5000;
	// The world age, also tick counter. Can count for billions of real-world
	// time so we are not in trouble.
	// Let's say that the game world runs at 60Ticks per second

	public IOTasks ioHandler;
	private WorldGenerator generator;
	final public WorldSize size;

	// RAM-eating monster
	public ChunksData chunksData;
	public ChunksHolders chunksHolder;

	// Heightmap management
	public RegionSummaries regionSummaries;

	// World-renderer backcall
	WorldRenderer renderer;

	// World logic thread
	ScheduledExecutorService logic;

	// Temporary entity list
	private BlockingQueue<Entity> entities = new LinkedBlockingQueue<Entity>();

	// Particles
	public ParticlesHolder particlesHolder;

	protected boolean client;

	protected WorldInfo worldInfo;

	public ConfigFile internalData;

	public World(WorldSize s)
	{
		size = s;
	}

	public World(WorldInfo info)
	{
		this(info.internalName, info.seed, info.getGenerator(), info.size);
		worldInfo = info;
	}

	public World(String name, String seed, WorldGenerator access, WorldSize s)
	{
		// Called by any initialisation code.
		this.name = name;
		this.seed = seed;
		size = s;
		generator = access;
		generator.initialize(this);
		chunksData = new ChunksData();
		chunksHolder = new ChunksHolders(this, chunksData);
		regionSummaries = new RegionSummaries(this);
		logic = Executors.newSingleThreadScheduledExecutor();
		folder = new File(GameDirectory.getGameFolderPath() + "/worlds/" + name);

		internalData = new ConfigFile(GameDirectory.getGameFolderPath() + "/worlds/" + name + "/internal.dat");
		internalData.load();
	}

	public File getFolderFile()
	{
		return folder;
	}

	public String getFolderPath()
	{
		return folder.getAbsolutePath();
	}

	public void linkWorldRenderer(WorldRenderer renderer)
	{
		this.renderer = renderer;
		particlesHolder = new ParticlesHolder();
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
		EntityImpl impl = (EntityImpl) entity;
		if (this instanceof WorldMaster)
			impl.entityID = nextEntityId();
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
				//entity.delete();
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
		try
		{
			Iterator<Entity> iter = this.getAllLoadedEntities();
			Entity entity;
			while (iter.hasNext())
			{
				entity = iter.next();
				if (entity instanceof EntityControllable && ((EntityControllable) entity).getController() != null && ((EntityControllable) entity).getController() instanceof ClientController)
					((EntityControllable) entity).tick(Client.getInstance());
				if (entity.getChunkHolder() != null && entity.getChunkHolder().isLoaded())
					entity.tick();
				//System.out.println(entity);
			}
			if (particlesHolder != null)
				particlesHolder.updatePhysics();
			// worldTime++;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
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
		return size.height * 32;
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#getSizeInChunks()
	 */
	@Override
	public int getSizeInChunks()
	{
		return size.sizeInChunks;
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#getSizeSide()
	 */
	@Override
	public double getSizeSide()
	{
		return size.sizeInChunks * 32;
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#getChunk(int, int, int, boolean)
	 */
	@Override
	public CubicChunk getChunk(int chunkX, int chunkY, int chunkZ, boolean load)
	{
		chunkX = chunkX % size.sizeInChunks;
		chunkZ = chunkZ % size.sizeInChunks;
		if (chunkX < 0)
			chunkX += size.sizeInChunks;
		if (chunkZ < 0)
			chunkZ += size.sizeInChunks;
		if (chunkY < 0)
			return null;
		if (chunkY >= size.height)
			return null;
		return chunksHolder.getChunk(chunkX, chunkY, chunkZ, load);
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
		chunkX = chunkX % size.sizeInChunks;
		chunkZ = chunkZ % size.sizeInChunks;
		if (chunkX < 0)
			chunkX += size.sizeInChunks;
		if (chunkZ < 0)
			chunkZ += size.sizeInChunks;
		if (chunkY < 0)
			chunkY = 0;
		//ioHandler.requestChunkUnload(chunkX, chunkY, chunkZ);
		chunksHolder.removeChunk(chunkX, chunkY, chunkZ, save);
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#isChunkLoaded(int, int, int)
	 */
	@Override
	public boolean isChunkLoaded(int chunkX, int chunkY, int chunkZ)
	{
		chunkX = chunkX % size.sizeInChunks;
		chunkZ = chunkZ % size.sizeInChunks;
		if (chunkX < 0)
			chunkX += size.sizeInChunks;
		if (chunkZ < 0)
			chunkZ += size.sizeInChunks;
		if (chunkY < 0)
			return false;
		if (chunkY >= size.height)
			return false;
		ChunkHolder h = chunksHolder.getChunkHolder(chunkX, chunkY, chunkZ, false);
		if (h == null)
			return false;
		else
			return h.isChunkLoaded(chunkX, chunkY, chunkZ);
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
		x = x % (size.sizeInChunks * 32);
		z = z % (size.sizeInChunks * 32);
		if (y < 0)
			y = 0;
		if (y > size.height * 32)
			y = size.height * 32;
		if (x < 0)
			x += size.sizeInChunks * 32;
		if (z < 0)
			z += size.sizeInChunks * 32;
		Chunk c = chunksHolder.getChunk(x / 32, y / 32, z / 32, load);
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
		regionSummaries.blockPlaced(x, y, z, i);

		x = x % (size.sizeInChunks * 32);
		z = z % (size.sizeInChunks * 32);
		if (y < 0)
			y = 0;
		if (y > size.height * 32)
			y = size.height * 32;
		if (x < 0)
			x += size.sizeInChunks * 32;
		if (z < 0)
			z += size.sizeInChunks * 32;
		Chunk c = chunksHolder.getChunk(x / 32, y / 32, z / 32, load);
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
						chunksHolder.markChunkDirty((x - 1) / 32, (y - 1) / 32, (z - 1) / 32);
					else if (z % 32 == 31)
						chunksHolder.markChunkDirty((x - 1) / 32, (y - 1) / 32, (z + 1) / 32);
					chunksHolder.markChunkDirty((x - 1) / 32, (y - 1) / 32, (z) / 32);
				}
				else if (y % 32 == 31)
				{
					if (z % 32 == 0)
						chunksHolder.markChunkDirty((x - 1) / 32, (y + 1) / 32, (z - 1) / 32);
					else if (z % 32 == 31)
						chunksHolder.markChunkDirty((x - 1) / 32, (y + 1) / 32, (z + 1) / 32);
					chunksHolder.markChunkDirty((x - 1) / 32, (y + 1) / 32, (z) / 32);
				}
				else
				{
					if (z % 32 == 0)
						chunksHolder.markChunkDirty((x - 1) / 32, (y) / 32, (z - 1) / 32);
					else if (z % 32 == 31)
						chunksHolder.markChunkDirty((x - 1) / 32, (y) / 32, (z + 1) / 32);
					chunksHolder.markChunkDirty((x - 1) / 32, (y) / 32, (z) / 32);
				}
			}
			else if (x % 32 == 31)
			{
				if (y % 32 == 0)
				{
					if (z % 32 == 0)
						chunksHolder.markChunkDirty((x + 1) / 32, (y - 1) / 32, (z - 1) / 32);
					else if (z % 32 == 31)
						chunksHolder.markChunkDirty((x + 1) / 32, (y - 1) / 32, (z + 1) / 32);
					chunksHolder.markChunkDirty((x + 1) / 32, (y - 1) / 32, (z) / 32);
				}
				else if (y % 32 == 31)
				{
					if (z % 32 == 0)
						chunksHolder.markChunkDirty((x + 1) / 32, (y + 1) / 32, (z - 1) / 32);
					else if (z % 32 == 31)
						chunksHolder.markChunkDirty((x + 1) / 32, (y + 1) / 32, (z + 1) / 32);
					chunksHolder.markChunkDirty((x + 1) / 32, (y + 1) / 32, (z) / 32);
				}
				else
				{
					if (z % 32 == 0)
						chunksHolder.markChunkDirty((x + 1) / 32, (y) / 32, (z - 1) / 32);
					else if (z % 32 == 31)
						chunksHolder.markChunkDirty((x + 1) / 32, (y) / 32, (z + 1) / 32);
					chunksHolder.markChunkDirty((x + 1) / 32, (y) / 32, (z) / 32);
				}
			}
			if (y % 32 == 0)
			{
				if (z % 32 == 0)
					chunksHolder.markChunkDirty((x) / 32, (y - 1) / 32, (z - 1) / 32);
				else if (z % 32 == 31)
					chunksHolder.markChunkDirty((x) / 32, (y - 1) / 32, (z + 1) / 32);
				chunksHolder.markChunkDirty((x) / 32, (y - 1) / 32, (z) / 32);
			}
			else if (y % 32 == 31)
			{
				if (z % 32 == 0)
					chunksHolder.markChunkDirty((x) / 32, (y + 1) / 32, (z - 1) / 32);
				else if (z % 32 == 31)
					chunksHolder.markChunkDirty((x) / 32, (y + 1) / 32, (z + 1) / 32);
				chunksHolder.markChunkDirty((x) / 32, (y + 1) / 32, (z) / 32);
			}
			else
			{
				if (z % 32 == 0)
					chunksHolder.markChunkDirty((x) / 32, (y) / 32, (z - 1) / 32);
				else if (z % 32 == 31)
					chunksHolder.markChunkDirty((x) / 32, (y) / 32, (z + 1) / 32);
				chunksHolder.markChunkDirty((x) / 32, (y) / 32, (z) / 32);
			}
		}
	}

	public void setDataAtWithoutUpdates(int x, int y, int z, int i, boolean load)
	{
		regionSummaries.blockPlaced(x, y, z, i);

		x = x % (size.sizeInChunks * 32);
		z = z % (size.sizeInChunks * 32);
		if (y < 0)
			y = 0;
		if (y > size.height * 32)
			y = size.height * 32;
		if (x < 0)
			x += size.sizeInChunks * 32;
		if (z < 0)
			z += size.sizeInChunks * 32;
		Chunk c = chunksHolder.getChunk(x / 32, y / 32, z / 32, load);
		if (c != null)
		{
			synchronized (c)
			{
				c.setDataAtWithoutUpdates(x % 32, y % 32, z % 32, i);
			}
		}
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
		chunksHolder.setChunk(chunk);
		if (renderer != null)
			renderer.modified();
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#reRender()
	 */
	@Override
	public synchronized void reRender()
	{
		ChunksIterator i = this.iterator();
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
	public void clear()
	{
		chunksHolder.clearAll();
		regionSummaries.clearAll();
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#save()
	 */
	@Override
	public void save()
	{
		System.out.println("Saving world");
		chunksHolder.saveAll();
		regionSummaries.saveAll();

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
		this.chunksHolder.destroy();
		this.regionSummaries.destroy();
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
	public ChunksIterator iterator()
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
		ChunksIterator it = this.iterator();
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

				if (((LoopingMathHelper.moduloDistance(chunk.chunkX, pCX, sizeInChunks) > chunksViewDistance + 1) || (LoopingMathHelper.moduloDistance(chunk.chunkZ, pCZ, sizeInChunks) > chunksViewDistance + 1) || (chunk.chunkY - pCY) > 3 || (chunk.chunkY - pCY) < -3))
				{
					if(chunk.chunkRenderData != null)
						chunk.chunkRenderData.markForDeletion();
					chunk.need_render.set(true);
					keep = false;
				}
			}
			if (!keep)
				it.remove();
		}
	}

	boolean raining;

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.WorldInterface#isRaining()
	 */
	@Override
	public boolean isRaining()
	{
		return raining;
	}

	AtomicLong veryLong = new AtomicLong();
	public DequePool dequesPool = new DequePool();

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
}
