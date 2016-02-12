package io.xol.chunkstories.world;

import static org.lwjgl.opengl.GL15.glDeleteBuffers;

import java.io.File;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.xol.chunkstories.GameDirectory;
import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.world.ChunksIterator;
import io.xol.chunkstories.api.world.WorldGenerator;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.FastConfig;
import io.xol.chunkstories.entity.Entity;
import io.xol.chunkstories.physics.particules.ParticlesHolder;
import io.xol.chunkstories.renderer.WorldRenderer;
import io.xol.chunkstories.tools.WorldTool;
import io.xol.chunkstories.world.io.IOTasks;
import io.xol.chunkstories.world.iterators.WorldChunksIterator;
import io.xol.chunkstories.world.summary.ChunkSummaries;
import io.xol.engine.math.LoopingMathHelper;
import io.xol.engine.misc.ConfigFile;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public abstract class World
{

	public String name;
	public String seed;
	public File folder;

	public long worldTime = 5000;
	// The world age, also tick counter. Can count for billions of real-world
	// time so we are not in trouble.
	// Let's say that the game world runs at 60Ticks per second

	public IOTasks ioHandler;
	public WorldGenerator generator;
	final public WorldSize size;

	// RAM-eating monster
	public ChunksData chunksData;
	public ChunksHolders chunksHolder;

	// Heightmap management
	public ChunkSummaries chunkSummaries;

	// World-renderer backcall
	WorldRenderer renderer;

	// World logic thread
	ScheduledExecutorService logic;

	// Temporary entity list
	//public List<Entity> entities = new ArrayList<Entity>();
	public BlockingQueue<Entity> entities = new LinkedBlockingQueue<Entity>();

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
		chunkSummaries = new ChunkSummaries(this);
		logic = Executors.newSingleThreadScheduledExecutor();
		folder = new File(GameDirectory.getGameFolderPath() + "/worlds/" + name);

		internalData = new ConfigFile(GameDirectory.getGameFolderPath() + "/worlds/" + name + "/internal.dat");
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
		// logic.start();
		logic.scheduleAtFixedRate(new Runnable()
		{
			public void run()
			{
				try
				{
					tick();
				}
				catch (Exception e)
				{
					System.out.println("Son excellence le fils de pute de thread silencieusement suicidaire de mes couilles");
					e.printStackTrace();
				}
			}
		}, 0, 16666, TimeUnit.MICROSECONDS);
	}

	public void addEntity(final Entity entity)
	{
		if (!client)
			entity.entityID = nextEntityId();
		entity.setHolder();
		this.entities.add(entity);
		/*ioHandler.requestChunkHolderRequest(entity.parentHolder, new IORequiringTask()
		{
			public boolean run(ChunkHolder holder)
			{
				holder.addEntity(entity);
				return true;
			}
		});*/
	}

	public void removeEntity(Entity entity)
	{
		Iterator<Entity> iter = entities.iterator();
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

	public void tick()
	{
		try
		{
			/*for (ChunkHolder holder : getAllLoadedChunksHolders())
			{
				//holder.tick();
			}*/
			//
			Iterator<Entity> iter = entities.iterator();
			Entity entity;
			while (iter.hasNext())
			{
				entity = iter.next();
				if (entity.parentHolder != null && entity.parentHolder.isLoaded())
					entity.update();
				//System.out.println(entity);
			}
			//
			if (particlesHolder != null)
				particlesHolder.updatePhysics();
			// worldTime++;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/*public List<Entity> getAllLoadedEntities()
	{
		List<Entity> entitiesToReturn = new ArrayList<Entity>();
		
		Iterator<Entity> iter = entities.iterator();
		while (iter.hasNext())
		{
			entitiesToReturn.add(iter.next());
		}
		return entitiesToReturn;
	}*/

	public Iterator<Entity> getAllLoadedEntities()
	{
		return entities.iterator();
	}

	public Entity getEntityByUUID(long entityID)
	{
		Iterator<Entity> ie = getAllLoadedEntities();
		Entity e;
		while (ie.hasNext())
		//for (Entity e : getAllLoadedEntities())
		{
			e = ie.next();
			if (e.entityID == entityID)
				return e;
		}
		return null;
	}

	public int getMaxHeight()
	{
		return size.height * 32;
	}

	public int getSizeInChunks()
	{
		return size.sizeInChunks;
	}

	public double getSizeSide()
	{
		return size.sizeInChunks * 32;
	}

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

	public void removeChunk(CubicChunk c, boolean save)
	{
		removeChunk(c.chunkX, c.chunkY, c.chunkZ, save);
	}

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
		chunksHolder.removeChunk(chunkX, chunkY, chunkZ);
	}

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

	public int getDataAt(int x, int y, int z)
	{
		return getDataAt(x, y, z, true);
	}

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
		CubicChunk c = chunksHolder.getChunk(x / 32, y / 32, z / 32, load);
		if (c != null)
			return c.getDataAt(x, y, z);
		return 0;
	}

	public void setDataAt(int x, int y, int z, int i)
	{
		setDataAt(x, y, z, i, false);
	}

	public void setDataAt(int x, int y, int z, int i, boolean load)
	{
		chunkSummaries.blockPlaced(x, y, z, i);

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
		CubicChunk c = chunksHolder.getChunk(x / 32, y / 32, z / 32, load);
		if (c != null)
		{
			synchronized (c)
			{
				c.setDataAt(x % 32, y % 32, z % 32, i);
				c.markDirty(true);
			}
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

	/**
	 * Call this function to force redrawing all the chunks
	 */
	public synchronized void reRender()
	{
		ChunksIterator i = this.iterator();
		CubicChunk c;
		while (i.hasNext())
		{
			c = i.next();
			c.need_render.set(true);
			c.requestable.set(true);
			c.vbo_size_normal = 0;
			c.vbo_size_complex = 0;
			c.vbo_size_water = 0;
		}
		/*
		for (CubicChunk c : this.getAllLoadedChunks())
		{
			c.need_render.set(true);
			c.requestable.set(true);
			c.vbo_size_normal = 0;
			c.vbo_size_complex = 0;
			c.vbo_size_water = 0;
		}*/
	}

	public void clear()
	{
		chunksHolder.clearAll();
		chunkSummaries.clearAll();
	}

	public void save()
	{
		chunksHolder.saveAll();
		chunkSummaries.saveAll();
		//if (!client)
		
		this.internalData.setProp("entities-ids-counter", veryLong.get());
		this.internalData.save();
		System.out.println("Saving world");
	}

	public enum WorldSize
	{
		TINY(32, "1x1km"), SMALL(64, "2x2km"), MEDIUM(128, "4x4km"), LARGE(512, "16x16km"), HUGE(2048, "64x64km");

		// Warning : this can be VERY ressource intensive as it will make a
		// 4294km2 map,
		// leading to enormous map sizes ( in the order of 10Gbs to 100Gbs )
		// when fully explored.

		WorldSize(int s, String n)
		{
			sizeInChunks = s;
			name = n;
		}

		public int sizeInChunks;
		public int height = 32;
		public String name;

		public static String getAllSizes()
		{
			String sizes = "";
			for (WorldSize s : WorldSize.values())
			{
				sizes = sizes + s.name() + ", " + s.name + " ";
			}
			return sizes;
		}

		public static WorldSize getWorldSize(String name)
		{
			name.toUpperCase();
			for (WorldSize s : WorldSize.values())
			{
				if (s.name().equals(name))
					return s;
			}
			return null;
		}
	}

	public void destroy()
	{
		this.chunksData.destroy();
		this.chunksHolder.destroy();
		this.chunkSummaries.destroy();
		this.logic.shutdown();
		if (!client)
		{
			this.internalData.setProp("entities-ids-counter", veryLong.get());
			this.internalData.save();
		}
		ioHandler.kill();
	}

	public ChunksIterator iterator()
	{
		return new WorldChunksIterator(this);
	}

	/*public List<CubicChunk> getAllLoadedChunks()
	{
		return chunksHolder.getAllLoadedChunks();
	}*/

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

	/**
	 * Unloads bits of the map not required by anyone
	 */
	public void trimRemovableChunks()
	{
		if(this instanceof WorldTool)
			System.out.println("omg this should not happen");
		ChunksIterator it = this.iterator();
		CubicChunk chunk;
		while (it.hasNext())
		{
			chunk = it.next();
			if(chunk == null)
			{
				it.remove();
				continue;
			}
			boolean keep = false;
			if (!keep && Client.controller != null)
			{
				keep = true;
				int sizeInChunks = this.getSizeInChunks();
				int chunksViewDistance = (int) (FastConfig.viewDistance / 32);
				int pCX = (int) (Client.controller.posX / 32);
				int pCY = (int) (Client.controller.posY / 32);
				int pCZ = (int) (Client.controller.posZ / 32);
				if (((LoopingMathHelper.moduloDistance(chunk.chunkX, pCX, sizeInChunks) > chunksViewDistance + 1) || (LoopingMathHelper.moduloDistance(chunk.chunkZ, pCZ, sizeInChunks) > chunksViewDistance + 1) || (chunk.chunkY - pCY) > 3
						|| (chunk.chunkY - pCY) < -3))
				{
					if (chunk.vbo_id != -1)
						glDeleteBuffers(chunk.vbo_id);
					chunk.need_render.set(true);
					keep = false;
				}
			}
			if (!keep)
				it.remove();
		}
	}

	boolean raining;

	public boolean isRaining()
	{
		return raining;
	}

	AtomicLong veryLong = new AtomicLong();

	public long nextEntityId()
	{
		return veryLong.getAndIncrement();
	}

	public void setWeather(boolean booleanProp)
	{
		raining = booleanProp;
	}

	public Location getDefaultSpawnLocation()
	{
		double dx = internalData.getDoubleProp("defaultSpawnX", 0.0);
		double dy = internalData.getDoubleProp("defaultSpawnY", 100.0);
		double dz = internalData.getDoubleProp("defaultSpawnZ", 0.0);
		return new Location(dx, dy, dz);
	}
}
