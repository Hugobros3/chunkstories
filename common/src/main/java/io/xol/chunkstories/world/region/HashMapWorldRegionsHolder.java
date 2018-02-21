package io.xol.chunkstories.world.region;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import io.xol.chunkstories.api.rendering.world.chunk.ChunkRenderable;
import io.xol.chunkstories.api.util.concurrency.Fence;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.api.world.chunk.ChunkHolder;
import io.xol.chunkstories.api.world.chunk.Region;
import io.xol.chunkstories.api.world.chunk.WorldUser;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.chunk.CubicChunk;
import io.xol.engine.concurrency.CompoundFence;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class HashMapWorldRegionsHolder
{
	private WorldImplementation world;

	private Semaphore noConcurrentRegionCreationDestruction = new Semaphore(1);
	private ConcurrentHashMap<Integer, RegionImplementation> regions = new ConcurrentHashMap<Integer, RegionImplementation>(8, 0.9f, 1);

	private final int sizeInRegions, heightInRegions;

	public HashMapWorldRegionsHolder(WorldImplementation world)
	{
		this.world = world;
		//this.chunksData = chunksData;
		heightInRegions = world.getWorldInfo().getSize().heightInChunks / 8;
		sizeInRegions = world.getWorldInfo().getSize().sizeInChunks / 8;
	}

	public Iterator<RegionImplementation> getLoadedRegions()
	{
		return regions.values().iterator();
	}

	public RegionImplementation getRegionChunkCoordinates(int chunkX, int chunkY, int chunkZ)
	{
		return getRegion(chunkX / 8, chunkY / 8, chunkZ / 8);
	}

	public RegionImplementation getRegion(int regionX, int regionY, int regionZ)
	{
		int key = (regionX * sizeInRegions + regionZ) * heightInRegions + regionY;
		//RegionLocation key = new RegionLocation(regionX, regionY, regionZ);
		return regions.get(key);
	}
	
	/**
	 * Only aquiring either the region or one of it's chunkholders should trigger a creation of a region
	 */
	private RegionImplementation getOrCreateRegion(int regionX, int regionY, int regionZ)
	{
		RegionImplementation holder = null;

		int key = (regionX * sizeInRegions + regionZ) * heightInRegions + regionY;
		//RegionLocation key = new RegionLocation(regionX, regionY, regionZ);

		//Lock to avoid any issues with another thread making another region while we handle this
		//Note lock was moved to a semaphore in public
		holder = regions.get(key);

		//Make a new region if we can't find it
		if (holder == null && regionY < heightInRegions * 8 && regionY >= 0)
		{
			holder = new RegionImplementation(world, regionX, regionY, regionZ, this);
		}

		return holder;
	}

	/**
	 * The Region constructor also loads it, and in the case of offline/immediate worlds it does so immediatly ( single-thread ), the issue
	 * is that while loading the entities it might try to read voxel data, triggering a chunk load operation, itself triggering a region lookup.
	 * This is an issue because if we add the Region to the hashmap after having executed the constructor we might end up in a infinite loop
	 * So to avoid that we do this
	 */
	protected void regionConstructorCallBack(RegionImplementation region)
	{
		int key = (region.getRegionX() * sizeInRegions + region.getRegionZ()) * heightInRegions + region.getRegionY();
		//RegionLocation key = new RegionLocation(region.regionX, region.regionY, region.regionZ);

		//System.out.println("Built region: "+region);
		//System.out.println("Key: "+key);
		
		//If it's not still saving an older version
		if (world.ioHandler.isDoneSavingRegion(region))
			regions.putIfAbsent(key, region);
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

	/*public void clearAll()
	{
		Iterator<RegionImplementation> i = regions.values().iterator();
		RegionImplementation holder;
		while (i.hasNext())
		{
			holder = i.next();
			if (holder != null)
			{
				holder.unload();
			}
		}
		regions.clear();
	}*/

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
	
	public Fence unloadsUselessData()
	{
		//We might want to wait for a few things
		CompoundFence compoundFence = new CompoundFence();
		
		//Prevents unloading a region whilst one of it's chunk holders is being aquired
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
	}

	/**
	 * Atomically grabs or create a region and registers the asked holder
	 */
	public ChunkHolder aquireChunkHolder(WorldUser user, int chunkX, int chunkY, int chunkZ)
	{
		if(chunkY < 0 || chunkY > world.getMaxHeight() / 32)
			return null;
		
		noConcurrentRegionCreationDestruction.acquireUninterruptibly();
		
		ChunkHolder holder = this.getOrCreateRegion(chunkX / 8, chunkY / 8, chunkZ / 8).getChunkHolder(chunkX, chunkY, chunkZ);
		boolean userAdded = holder.registerUser(user);
		
		noConcurrentRegionCreationDestruction.release();
		
		return userAdded ? holder : holder;
	}

	public RegionImplementation aquireRegion(WorldUser user, int regionX, int regionY, int regionZ)
	{
		if(regionY < 0 || regionY > world.getMaxHeight() / 256)
			return null;
		
		noConcurrentRegionCreationDestruction.acquireUninterruptibly();
		
		RegionImplementation region = this.getOrCreateRegion(regionX, regionY, regionZ);
		boolean userAdded = region.registerUser(user);
		
		noConcurrentRegionCreationDestruction.release();
		
		return userAdded ? region : null;
	}
	
	/**
	 * Callback by the holder's unload() method to remove himself from this list.
	 */
	public void removeRegion(RegionImplementation region)
	{
		int key = (region.getRegionX() * sizeInRegions + region.getRegionZ()) * heightInRegions + region.getRegionY();
		//regions.remove(new RegionLocation(region.getRegionX(), region.getRegionY(), region.getRegionZ()));
		regions.remove(key);
	}
}
