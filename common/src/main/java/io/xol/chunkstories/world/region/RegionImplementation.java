package io.xol.chunkstories.world.region;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.util.CompoundIterator;
import io.xol.chunkstories.api.util.IterableIterator;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.api.world.chunk.ChunksIterator;
import io.xol.chunkstories.api.world.chunk.Region;
import io.xol.chunkstories.api.world.chunk.WorldUser;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.chunk.ChunkHolderImplementation;
import io.xol.chunkstories.world.chunk.CompressedData;
import io.xol.chunkstories.world.chunk.CubicChunk;
import io.xol.chunkstories.world.io.IOTasks.IOTask;
import io.xol.chunkstories.world.region.format.CSFRegionFile;
//import io.xol.chunkstories.world.region.format.CSFRegionFile0x2C;
import io.xol.engine.concurrency.SafeWriteLock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class RegionImplementation implements Region
{
	public final WorldImplementation world;
	public final int regionX, regionY, regionZ;
	public final long uuid;
	//private final HashMapWorldRegionsHolder worldChunksHolder;
	
	protected Collection<CubicChunk> loadedChunks = ConcurrentHashMap.newKeySet();//new LinkedBlockingQueue<CubicChunk>();
	
	private final Set<WorldUser> users = ConcurrentHashMap.newKeySet();//new HashSet<WorldUser>();
	private final Lock usersLock = new ReentrantLock();
	
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
	//private Set<Entity> localEntities = ConcurrentHashMap.newKeySet();

	private static Random random = new Random();

	public RegionImplementation(WorldImplementation world, int regionX, int regionY, int regionZ, HashMapWorldRegionsHolder worldChunksHolder)
	{
		this.world = world;
		this.regionX = regionX;
		this.regionY = regionY;
		this.regionZ = regionZ;
		//this.worldChunksHolder = worldChunksHolder;

		if(regionX < 0 || regionY < 0 || regionZ < 0)
			throw new RuntimeException("Regions aren't allowed negative coordinates.");
		
		//Initialize slots
		chunkHolders = new ChunkHolderImplementation[512];
		for (int i = 0; i < 8; i++)
			for (int j = 0; j < 8; j++)
				for (int k = 0; k < 8; k++)
					chunkHolders[i * 64 + j *8 + k] = new ChunkHolderImplementation(this, loadedChunks, regionX * 8 + i, regionY * 8 + j, regionZ * 8 + k);

		//Unique UUID
		uuid = random.nextLong();

		worldChunksHolder.regionConstructorCallBack(this);

		//Set the initial cooldown delay
		unloadCooldown.set(System.currentTimeMillis());

		//Only the WorldMaster has a concept of files
		if (world instanceof WorldMaster)
		{
			handler = CSFRegionFile.determineVersionAndCreate(this);//new CSFRegionFile0x2D(this);
			world.ioHandler.requestRegionLoad(this);
		}
		else
		{
			handler = null;
			isDiskDataLoaded.set(true);
		}
	}

	@Override
	public IterableIterator<WorldUser> getChunkUsers()
	{
		return new IterableIterator<WorldUser>()
		{
			Iterator<WorldUser> i = users.iterator();

			@Override
			public boolean hasNext()
			{
				return i.hasNext();
			}

			@Override
			public WorldUser next()
			{
				return i.next();
			}

		};
	}

	@Override
	public boolean registerUser(WorldUser user)
	{
		try {
			usersLock.lock();
			return users.add(user);
		} finally {
			usersLock.unlock();
		}
	}

	@Override
	/**
	 * Unregisters user and if there is no remaining user, unloads the chunk
	 */
	public boolean unregisterUser(WorldUser user)
	{
		try {
			usersLock.lock();
			users.remove(user);
		
			if(users.isEmpty())
			{
				//unloadChunk();
				return true;
			}
			
			return false;
		} finally {
			usersLock.unlock();
		}
		
	}

	public int countUsers()
	{
		return users.size();
	}
	
	public CompressedData getCompressedData(int chunkX, int chunkY, int chunkZ)
	{
		return chunkHolders[(chunkX & 7) * 64 + (chunkY & 7) * 8 + (chunkZ & 7)].getCompressedData();
	}

	@Override
	public CubicChunk getChunk(int chunkX, int chunkY, int chunkZ)
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

		//world.entitiesLock.unlock();

		//Remove the reference in the world to this
		this.getWorld().getRegionsHolder().removeRegion(this);
	}

	@Override
	public IOTask save()
	{
		return world.ioHandler.requestRegionSave(this);
	}

	@Override
	public IOTask unloadAndSave()
	{
		unload();
		return world.ioHandler.requestRegionSave(this);
	}

	@Override
	public String toString()
	{
		return "[Region rx:" + regionX + " ry:" + regionY + " rz:" + regionZ + " uuid: " + uuid + "loaded?:" + isDiskDataLoaded.get() + " u:" + unloadedFlag + " chunks: " + "NULL" + " entities:" + "tbf" + "]";
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
						
						if(chunk.compr_uncomittedBlockModifications.get() > 0)
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
	public IterableIterator<Entity> getEntitiesWithinRegion()
	{
		List<Iterator<Entity>> listOfIterators = new ArrayList<Iterator<Entity>>();
		for(int a = 0; a < 8; a++)
			for(int b = 0; b < 8; b++)
				for(int c = 0; c < 8; c++) {
					Chunk chunk = this.getChunk(a, b, c);
					if(chunk != null)
						listOfIterators.add(chunk.getEntitiesWithinChunk());
				}
		
		return new CompoundIterator<Entity>(listOfIterators);
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
		
		//if(this.regionY == 0)
		//	System.out.println(usedChunks + " vs " + this.countUsers());
		
		return usedChunks == 0 && this.countUsers() == 0;
	}
}
