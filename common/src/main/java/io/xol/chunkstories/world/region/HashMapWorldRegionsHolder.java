//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.region;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.xol.chunkstories.api.rendering.world.chunk.ChunkRenderable;
import io.xol.chunkstories.api.util.concurrency.Fence;
import io.xol.chunkstories.api.world.WorldUser;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.api.world.chunk.ChunkHolder;
import io.xol.chunkstories.api.world.region.Region;
import io.xol.chunkstories.util.concurrency.CompoundFence;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.chunk.CubicChunk;

public class HashMapWorldRegionsHolder
{
	private WorldImplementation world;

	//private Semaphore noConcurrentRegionCreationDestruction = new Semaphore(1);
	private final ReadWriteLock regionsLock = new ReentrantReadWriteLock();
	private final Map<Integer, RegionImplementation> regions = new HashMap<>(); //new ConcurrentHashMap<Integer, RegionImplementation>(8, 0.9f, 1);

	private final int sizeInRegions, heightInRegions;

	public HashMapWorldRegionsHolder(WorldImplementation world)
	{
		this.world = world;
		//this.chunksData = chunksData;
		heightInRegions = world.getWorldInfo().getSize().heightInChunks / 8;
		sizeInRegions = world.getWorldInfo().getSize().sizeInChunks / 8;
	}

	public Iterator<RegionImplementation> internalGetLoadedRegions() {
		//Iterators are sort of unsafe so we quickly build a list and let them iterate that
		try {
			regionsLock.readLock().lock();
			List<RegionImplementation> list = new LinkedList<>();
			for(RegionImplementation r : regions.values()) {
				list.add(r);
			}
		
			return list.iterator();
		} finally {
			regionsLock.readLock().unlock();
		}
		//return regions.values().iterator();
	}

	public RegionImplementation getRegionChunkCoordinates(int chunkX, int chunkY, int chunkZ) {
		return getRegion(chunkX / 8, chunkY / 8, chunkZ / 8);
	}

	public RegionImplementation getRegion(int regionX, int regionY, int regionZ) {
		try {
			regionsLock.readLock().lock();
			int key = (regionX * sizeInRegions + regionZ) * heightInRegions + regionY;
			return regions.get(key);
		} finally {
			regionsLock.readLock().unlock();
		}
	}

	long prout = 0;
	
	public CubicChunk getChunk(int chunkX, int chunkY, int chunkZ)
	{
		RegionImplementation holder = getRegionChunkCoordinates(chunkX, chunkY, chunkZ);
		if (holder != null)
		{
			//System.out.println(regions.size());
			return holder.getChunk(chunkX, chunkY, chunkZ);
		}
		return null;
	}

	public Fence saveAll()
	{
		CompoundFence allRegionsFences = new CompoundFence();
		
		Iterator<RegionImplementation> i = regions.values().iterator();
		Region region;
		while (i.hasNext())
		{
			region = i.next();
			if (region != null)
			{
				allRegionsFences.add(region.save());
			}
		}
		
		return allRegionsFences;
	}

	public void markChunkForReRender(int chunkX, int chunkY, int chunkZ)
	{
		int worldSizeInChunks = world.getWorldInfo().getSize().sizeInChunks;
		if (chunkX < 0)
			chunkX += worldSizeInChunks;
		if (chunkY < 0)
			chunkY += worldSizeInChunks;
		if (chunkZ < 0)
			chunkZ += worldSizeInChunks;
		chunkX = chunkX % worldSizeInChunks;
		chunkZ = chunkZ % worldSizeInChunks;
		
		if (chunkY < 0 || chunkY >= worldSizeInChunks)
			return;
		
		Chunk c = getChunk(chunkX, chunkY, chunkZ);
		if (c != null && c instanceof ChunkRenderable)
			((ChunkRenderable) c).meshUpdater().requestMeshUpdate();
	}

	public void destroy()
	{
		regions.clear();
	}

	@Override
	public String toString()
	{
		return "[ChunksHolder: " + regions.size() + " Chunk Holders loaded]";
	}
	
	public String getStats()
	{
		return countChunks() + " (lr: " + regions.size() + " )";
	}

	public int countChunks()
	{
		int c = 0;
		Iterator<Chunk> i = world.getAllLoadedChunks();
		while (i.hasNext())
		{
			i.next();
			c++;
		}
		return c;
	}

	public int countChunksWithData()
	{
		int c = 0;
		Iterator<Chunk> i = world.getAllLoadedChunks();
		while (i.hasNext())
		{
			if (!i.next().isAirChunk())
				c++;
		}
		return c;
	}
	
	/** Atomically adds an user to a region itself, and creates it if it was previously unused */
	public RegionImplementation acquireRegion(WorldUser user, int regionX, int regionY, int regionZ) {
		if (regionY < 0 || regionY > world.getMaxHeight() / 256)
			return null;

		int key = (regionX * sizeInRegions + regionZ) * heightInRegions + regionY;
		
		this.regionsLock.writeLock().lock();

		RegionImplementation region = regions.get(key);
		boolean fresh = false;
		if(region == null) {
			region = new RegionImplementation(world, regionX, regionY, regionZ);
			fresh = true;
		}
		
		boolean userAdded = region.registerUser(user);

		if(fresh)
			regions.put(key, region);
		
		this.regionsLock.writeLock().unlock();

		return userAdded ? region : null;
	}

	/** Atomically adds an user to a region's chunk, and creates the region if it was previously unused */
	public ChunkHolder acquireChunkHolder(WorldUser user, int chunkX, int chunkY, int chunkZ) {
		if (chunkY < 0 || chunkY > world.getMaxHeight() / 32)
			return null;

		int regionX = chunkX >> 3;
		int regionY = chunkY >> 3;
		int regionZ = chunkZ >> 3;
		int key = (regionX * sizeInRegions + regionZ) * heightInRegions + regionY;
		
		this.regionsLock.writeLock().lock();

		RegionImplementation region = regions.get(key);
		boolean fresh = false;
		if(region == null) {
			region = new RegionImplementation(world, regionX, regionY, regionZ);
			fresh = true;
		}
		
		ChunkHolder chunkHolder = region.getChunkHolder(chunkX, chunkY, chunkZ);
		boolean userAdded = chunkHolder.registerUser(user);

		if(fresh)
			regions.put(key, region);
		
		this.regionsLock.writeLock().unlock();

		return userAdded ? chunkHolder : chunkHolder;
	}
	
	/*public Fence unloadsUselessData()
	{
		//We might want to wait for a few things
		CompoundFence compoundFence = new CompoundFence();
		
		//Prevents unloading a region whilst one of it's chunk holders is being acquired
		noConcurrentRegionCreationDestruction.acquireUninterruptibly();
		
		//Iterates over loaded regions and unloads unused ones
		Iterator<RegionImplementation> regionsIterator = this.getLoadedRegions();
		while (regionsIterator.hasNext())
		{
			RegionImplementation region = regionsIterator.next();
			//Processes users, remove null ones
			region.unloadsUnusedChunks();
			
			//If no users have registered for any chunks
			if(region.isUnused() && region.canBeUnloaded())
			{
				// You actually don't want to stop saving, because entities move arround and you could fuck your representation of the world else
				// Either it's all read-only, either it's stream-saving everything
				// This is the only way to ensure all entities end up in a single region file and we don't have uuid conflicts
				if(world instanceof WorldMaster)
					compoundFence.add(region.unloadAndSave());
				else
					region.unload(); //Immediate
			}
		}
		
		noConcurrentRegionCreationDestruction.release();
		
		return compoundFence;
	}*/
	
	/**
	 * Callback by the holder's unload() method to remove himself from this list.
	 */
	void removeRegion(RegionImplementation region)
	{
		this.regionsLock.writeLock().lock();
		int key = (region.getRegionX() * sizeInRegions + region.getRegionZ()) * heightInRegions + region.getRegionY();
		regions.remove(key);
		this.regionsLock.writeLock().unlock();
	}
}
