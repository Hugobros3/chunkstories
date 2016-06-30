package io.xol.chunkstories.world.chunk;

import io.xol.chunkstories.api.world.Chunk;
import io.xol.chunkstories.api.world.Region;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.engine.concurrency.SimpleLock;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class WorldChunksHolder
{
	private WorldImplementation world;

	private SimpleLock worldDataLock = new SimpleLock();
	private ConcurrentHashMap<ChunkHolderKey, ChunkHolder> chunkHolders = new ConcurrentHashMap<ChunkHolderKey, ChunkHolder>(8, 0.9f, 1);

	private final int sizeInRegions, heightInRegions;

	private class ChunkHolderKey
	{
		public int regionX, regionY, regionZ;

		public ChunkHolderKey(int x, int y, int z)
		{
			regionX = x;
			regionY = y;
			regionZ = z;
		}

		@Override
		public boolean equals(Object o)
		{
			if(!(o instanceof ChunkHolderKey))
				return false;
			ChunkHolderKey chk = (ChunkHolderKey)o;
			boolean equals = chk.regionX == regionX && chk.regionY == regionY && chk.regionZ == regionZ;
			//System.out.println("checking if chk == !" + equals);
			return equals;
		}
		
		@Override
		public int hashCode()
		{
			int address = (regionX * sizeInRegions + regionZ) * heightInRegions + regionY;
			//System.out.println("hashCode == "+address);
			return address;
		}
	}

	public WorldChunksHolder(WorldImplementation world)
	{
		this.world = world;
		//this.chunksData = chunksData;
		heightInRegions = world.getWorldInfo().getSize().heightInChunks / 8;
		sizeInRegions = world.getWorldInfo().getSize().sizeInChunks / 8;
	}

	public Iterator<ChunkHolder> getLoadedChunkHolders()
	{
		return chunkHolders.values().iterator();
	}
	
	public ChunkHolder getChunkHolderChunkCoordinates(int chunkX, int chunkY, int chunkZ, boolean requestLoadIfAbsent)
	{
		return getChunkHolderRegionCoordinates(chunkX / 8, chunkY / 8, chunkZ / 8, requestLoadIfAbsent);
	}
	
	/**
	 * This method is set to private because it creates an empty ChunkHolder and all ChunkHolders should always contain at least one chunk.
	 * @return
	 */
	public ChunkHolder getChunkHolderRegionCoordinates(int regionX, int regionY, int regionZ, boolean requestLoadIfAbsent)
	{
		ChunkHolder holder = null;
		ChunkHolderKey key = new ChunkHolderKey(regionX, regionY, regionZ);
		
		//Lock to avoid any issues with another thread making another holder while we handle this
		worldDataLock.lock();
		holder = chunkHolders.get(key);
		
		//Make a new chunkHolder if we can't find it
		if (requestLoadIfAbsent && holder == null && regionY < heightInRegions * 8 && regionY >= 0)
		{
			holder = new ChunkHolder(world, regionX, regionY, regionZ);
			chunkHolders.putIfAbsent(key, holder);
		}
		worldDataLock.unlock();
		
		return holder;
	}
	
	public void setChunk(Chunk chunk)
	{
		ChunkHolder holder = getChunkHolderChunkCoordinates(chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ(), true);

		if (holder != null)
			holder.setChunk(chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ(), chunk);
		else //destroy it to avoid memory leaks
			if (chunk != null)
				chunk.destroy();
	}
	
	public void removeChunk(int chunkX, int chunkY, int chunkZ, boolean saveBeforeRemoving)
	{
		ChunkHolder holder = getChunkHolderChunkCoordinates(chunkX, chunkY, chunkZ, false);
		//Has removing the chunk made the holder empty ?
		boolean emptyHolder = false;
		if (holder != null)
			emptyHolder = holder.removeChunk(chunkX, chunkY, chunkZ);
		
		if (emptyHolder)
		{
			if(saveBeforeRemoving)
				holder.save();
			holder.unload();
			//System.out.println("Remove holder: "+holder);
			chunkHolders.remove(new ChunkHolderKey(chunkX / 8, chunkY / 8, chunkZ / 8 ));
		}
		//world.ioHandler.notifyChunkUnload(chunkX, chunkY, chunkZ);
	}
	
	public Chunk getChunk(int chunkX, int chunkY, int chunkZ, boolean requestLoadIfAbsent)
	{
		ChunkHolder holder = getChunkHolderChunkCoordinates(chunkX, chunkY, chunkZ, requestLoadIfAbsent);
		if (holder != null)
		{
			return holder.getChunk(chunkX, chunkY, chunkZ, requestLoadIfAbsent);
		}
		return null;
	}
	
	public void saveAll()
	{
		Iterator<ChunkHolder> i = chunkHolders.values().iterator();
		Region holder;
		while(i.hasNext())
		{
			holder = i.next();
			if (holder != null)
			{
				holder.save();
			}
		}
	}

	public void clearAll()
	{
		Iterator<ChunkHolder> i = chunkHolders.values().iterator();
		ChunkHolder holder;
		while(i.hasNext())
		{
			holder = i.next();
			if (holder != null)
			{
				holder.unloadAll();
			}
		}
		chunkHolders.clear();
	}

	public void markChunkForReRender(int chunkX, int chunkY, int chunkZ)
	{
		int sic = world.getWorldInfo().getSize().sizeInChunks;
		if (chunkX < 0)
			chunkX += sic;
		if (chunkY < 0)
			chunkY += sic;
		if (chunkZ < 0)
			chunkZ += sic;
		chunkX = chunkX % sic;
		if (chunkY < 0 || chunkY >= sic)
			return;
		// chunkY = chunkY % sic;
		chunkZ = chunkZ % sic;
		Chunk c = getChunk(chunkX, chunkY, chunkZ, false);
		if (c != null && c instanceof ChunkRenderable)
			((ChunkRenderable) c).markForReRender();
	}

	public void destroy()
	{
		chunkHolders.clear();
	}
	
	@Override
	public String toString()
	{
		return "[ChunksHolder: "+chunkHolders.size()+" Chunk Holders loaded]";
	}
	public int countChunks()
	{
		int c = 0;
		Iterator<Chunk> i = world.getAllLoadedChunks();
		while(i.hasNext())
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
		while(i.hasNext())
		{
			if(!i.next().isAirChunk())
				c++;
		}
		return c;
	}
}
