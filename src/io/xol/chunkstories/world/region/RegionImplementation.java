package io.xol.chunkstories.world.region;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityVoxel;
import io.xol.chunkstories.api.entity.interfaces.EntityUnsaveable;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.api.world.chunk.ChunksIterator;
import io.xol.chunkstories.api.world.chunk.Region;
import io.xol.chunkstories.api.world.chunk.WorldUser;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.chunk.ChunkHolderImplementation;
import io.xol.chunkstories.world.chunk.CubicChunk;
import io.xol.engine.concurrency.SafeWriteLock;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class RegionImplementation implements Region
{
	public final WorldImplementation world;
	public final int regionX, regionY, regionZ;
	public final long uuid;
	private final WorldRegionsHolder worldChunksHolder;
	
	protected Collection<CubicChunk> loadedChunks = ConcurrentHashMap.newKeySet();//new LinkedBlockingQueue<CubicChunk>();
	private Set<WeakReference<WorldUser>> users = new HashSet<WeakReference<WorldUser>>();

	//Only relevant on Master worlds
	public final CSFRegionFile handler;

	private AtomicLong unloadCooldown = new AtomicLong();
	private boolean unloadedFlag = false;

	//TODO find a clean way to let IOTaks fiddle with this
	public SafeWriteLock chunksArrayLock = new SafeWriteLock();

	// Holds 8x8x8 CubicChunks
	private ChunkHolderImplementation[] chunkHolders;
	private AtomicBoolean isDiskDataLoaded = new AtomicBoolean(false);

	//Local entities
	private Set<Entity> localEntities = ConcurrentHashMap.newKeySet();

	private static Random random = new Random();

	public RegionImplementation(WorldImplementation world, int regionX, int regionY, int regionZ, WorldRegionsHolder worldChunksHolder)
	{
		this.world = world;
		this.regionX = regionX;
		this.regionY = regionY;
		this.regionZ = regionZ;
		this.worldChunksHolder = worldChunksHolder;

		//Initialize slots
		chunkHolders = new ChunkHolderImplementation[512];
		for (int i = 0; i < 8; i++)
			for (int j = 0; j < 8; j++)
				for (int k = 0; k < 8; k++)
					chunkHolders[i * 64 + j *8 + k] = new ChunkHolderImplementation(this, loadedChunks, i, j, k);

		//Unique UUID
		uuid = random.nextLong();

		worldChunksHolder.regionConstructorCallBack(this);

		//Set the initial cooldown delay
		unloadCooldown.set(System.currentTimeMillis());

		//Only the WorldMaster has a concept of files
		if (world instanceof WorldMaster)
		{
			handler = new CSFRegionFile(this);
			world.ioHandler.requestRegionLoad(this);
		}
		else
		{
			handler = null;
			isDiskDataLoaded.set(true);
		}
	}

	@Override
	public Iterator<WorldUser> getChunkUsers()
	{
		return new Iterator<WorldUser>()
		{
			Iterator<WeakReference<WorldUser>> i = users.iterator();
			WorldUser user;

			@Override
			public boolean hasNext()
			{
				while(user == null && i.hasNext())
				{
					user = i.next().get();
				}
				return user != null;
			}

			@Override
			public WorldUser next()
			{
				hasNext();
				WorldUser u = user;
				user = null;
				return u;
			}

		};
	}

	@Override
	public boolean registerUser(WorldUser user)
	{
		Iterator<WeakReference<WorldUser>> i = users.iterator();
		while (i.hasNext())
		{
			WeakReference<WorldUser> w = i.next();
			WorldUser u = w.get();
			if (u == null)
				i.remove();
			else if (u != null && u.equals(user))
				return false;
		}
		
		users.add(new WeakReference<WorldUser>(user));
		
		return true;
	}

	@Override
	/**
	 * Unregisters user and if there is no remaining user, unloads the chunk
	 */
	public boolean unregisterUser(WorldUser user)
	{
		Iterator<WeakReference<WorldUser>> i = users.iterator();
		while (i.hasNext())
		{
			WeakReference<WorldUser> w = i.next();
			WorldUser u = w.get();
			if (u == null)
				i.remove();
			else if (u != null && u.equals(user))
				i.remove();
		}
		
		if(users.isEmpty())
		{
			//unloadChunk();
			return true;
		}
		
		return false;
	}

	public int countUsers()
	{
		int c = 0;
		
		Iterator<WeakReference<WorldUser>> i = users.iterator();
		while (i.hasNext())
		{
			WeakReference<WorldUser> w = i.next();
			WorldUser u = w.get();
			if (u == null)
				i.remove();
			else
			{
				//Remainings of a very long and drawn out garbage collection debugging session :/
				/*if(u instanceof ServerPlayer)
				{
					ServerPlayer p = (ServerPlayer)u;
					System.out.println("Region used by "+p);
				}*/
				//System.out.println(u);
				c++;
			}
		}
		
		return c;
	}
	
	public byte[] getCompressedData(int chunkX, int chunkY, int chunkZ)
	{
		return chunkHolders[(chunkX & 7) * 64 + (chunkY & 7) * 8 + (chunkZ & 7)].getCompressedData();
	}

	@Override
	public Chunk getChunk(int chunkX, int chunkY, int chunkZ)
	{
		return chunkHolders[(chunkX & 7) * 64 + (chunkY & 7) * 8 + (chunkZ & 7)].getChunk();
	}

	@Override
	public ChunkHolderImplementation getChunkHolder(int chunkX, int chunkY, int chunkZ)
	{
		return chunkHolders[(chunkX & 7) * 64 + (chunkY & 7) * 8 + (chunkZ & 7)];
	}
	
	@Override
	public boolean isChunkLoaded(int chunkX, int chunkY, int chunkZ)
	{
		return chunkHolders[(chunkX & 7) * 64 + (chunkY & 7) * 8 + (chunkZ & 7)].isChunkLoaded();
	}

	public ChunksIterator iterator()
	{
		return new ChunksIterator()
				{

					Iterator<CubicChunk> i = loadedChunks.iterator();
			
					@Override
					public boolean hasNext()
					{
						return i.hasNext();
					}

					@Override
					public Chunk next()
					{
						return i.next();
					}
			
				};
	}

	@Override
	public boolean isDiskDataLoaded()
	{
		return isDiskDataLoaded.get();
	}

	public boolean isUnloaded()
	{
		return unloadedFlag;
	}

	public void setDiskDataLoaded(boolean b)
	{
		isDiskDataLoaded.set(b);
	}

	public void unload()
	{
		//Before unloading the holder we want to make sure we finish all saving operations
		if (handler != null)
			handler.finishSavingOperations();

		//Set unloaded flag to true so we are not using again an unloaded holder
		unloadedFlag = true;

		//No need to unload chunks, this is assumed when we unload the holder
		//unloadAllChunks();

		world.entitiesLock.writeLock().lock();

		//int countRemovedEntities = 0;

		Iterator<Entity> i = this.getEntitiesWithinRegion();
		while (i.hasNext())
		{
			Entity entity = i.next();
			//i.remove();

			//Skip entities that shouldn't be saved
			if ((entity instanceof EntityUnsaveable && !((EntityUnsaveable) entity).shouldSaveIntoRegion()))
				continue;

			//System.out.println("Unloading entity"+entity+" currently in chunk holder "+this);

			//We keep the inner reference so serialization can still write entities contained within
			world.removeEntityFromList(entity);
			//countRemovedEntities++;
		}

		world.entitiesLock.writeLock().unlock();
		//world.entitiesLock.unlock();

		//Remove the reference in the world to this
		this.getWorld().getRegionsHolder().removeRegion(this);
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.world.chunk.Region#generateAll()
	 */
	public void generateAll()
	{
		// Generate terrain for the chunk holder !
		Chunk chunk;
		for (int a = 0; a < 8; a++)
			for (int b = 0; b < 8; b++)
				for (int c = 0; c < 8; c++)
				{
					//CubicChunk chunk = data[a][b][c];
					int cx = this.regionX * 8 + a;
					int cy = this.regionY * 8 + b;
					int cz = this.regionZ * 8 + c;
					chunk = world.getGenerator().generateChunk(this, cx, cy, cz);

					if (chunk == null)
						System.out.println("Notice : generator " + world.getGenerator() + " produced a null chunk.");

					this.chunkHolders[a * 64 + b * 8 + c].setChunk((CubicChunk) chunk);
					//compressChunkData(data[a][b][c]);
				}

		compressAll();
	}

	@Override
	public void save()
	{
		world.ioHandler.requestRegionSave(this);
	}

	@Override
	public void unloadAndSave()
	{
		unload();
		world.ioHandler.requestRegionSave(this);
	}

	@Override
	public String toString()
	{
		return "[Region rx:" + regionX + " ry:" + regionY + " rz:" + regionZ + " uuid: " + uuid + "loaded?:" + isDiskDataLoaded.get() + " u:" + unloadedFlag + " chunks: " + "NULL" + " entities:" + this.localEntities.size() + "]";
	}

	public void compressAll()
	{
		for (int a = 0; a < 8; a++)
			for (int b = 0; b < 8; b++)
				for (int c = 0; c < 8; c++)
					chunkHolders[a * 64 + b * 8 + c].compressChunkData();
	}

	public void compressChangedChunks()
	{
		for (int a = 0; a < 8; a++)
			for (int b = 0; b < 8; b++)
				for (int c = 0; c < 8; c++)
				{
					if (chunkHolders[a * 64 + b * 8 + c].getChunk() != null)
					{
						CubicChunk chunk = chunkHolders[a * 64 + b * 8 + c].getChunk();
						
						if(chunk.unsavedBlockModifications.get() > 0)
						//if (chunk.lastModification.get() > chunk.lastModificationSaved.get())
							chunkHolders[a * 64 + b * 8 + c].compressChunkData();
					}
				}
	}

	@Override
	public int getNumberOfLoadedChunks()
	{
		int count = 0;

		for (int a = 0; a < 8; a++)
			for (int b = 0; b < 8; b++)
				for (int c = 0; c < 8; c++)
					if (chunkHolders[a * 64 + b * 8 + c].isChunkLoaded())
						count++;

		return count;
	}

	@Override
	public int getRegionX()
	{
		return regionX;
	}

	@Override
	public int getRegionY()
	{
		return regionY;
	}

	@Override
	public int getRegionZ()
	{
		return regionZ;
	}

	@Override
	public Iterator<Entity> getEntitiesWithinRegion()
	{
		return localEntities.iterator();
	}

	@Override
	public boolean removeEntityFromRegion(Entity entity)
	{
		return localEntities.remove(entity);
	}

	@Override
	public boolean addEntityToRegion(Entity entity)
	{
		return localEntities.add(entity);
	}

	public void resetUnloadCooldown()
	{
		unloadCooldown.set(System.currentTimeMillis());
	}

	public boolean canBeUnloaded()
	{
		//Don't unload it until it has been loaded for 10s
		return this.isDiskDataLoaded() && (System.currentTimeMillis() - this.unloadCooldown.get() > 10 * 1000L);
	}

	public WorldImplementation getWorld()
	{
		return world;
	}

	@Override
	public EntityVoxel getEntityVoxelAt(int worldX, int worldY, int worldZ)
	{
		Iterator<Entity> entities = this.getEntitiesWithinRegion();
		while (entities.hasNext())
		{
			Entity entity = entities.next();
			if (entity != null && entity instanceof EntityVoxel && ((int) entity.getLocation().getX() == worldX) && ((int) entity.getLocation().getY() == worldY) && ((int) entity.getLocation().getZ() == worldZ))
				return (EntityVoxel) entity;
		}
		return null;
	}

	/**
	 * Unloads unused chunks, returns true if all chunks were unloaded
	 */
	public boolean unloadsUnusedChunks()
	{
		int loadedChunks = 0;
		
		for (int a = 0; a < 8; a++)
			for (int b = 0; b < 8; b++)
				for (int c = 0; c < 8; c++)
				{
					chunkHolders[a * 64 + b * 8 + c].unloadsIfUnused();
					if(chunkHolders[a * 64 + b * 8 + c].isChunkLoaded())
						loadedChunks++;
				}
		
		return loadedChunks == 0;
	}

	@Override
	/**
	 * Returns true if no one uses the region or one of it's chunk holders
	 */
	public boolean isUnused()
	{
		int usedChunks = 0;
		
		for (int a = 0; a < 8; a++)
			for (int b = 0; b < 8; b++)
				for (int c = 0; c < 8; c++)
				{
					chunkHolders[a * 64 + b * 8 + c].unloadsIfUnused();
					if(chunkHolders[a * 64 + b * 8 + c].isChunkLoaded() || chunkHolders[a * 64 + b * 8 + c].countUsers() > 0)
						usedChunks++;
				}
		
		return usedChunks == 0 && this.countUsers() == 0;
	}
}
