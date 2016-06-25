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

	private final int s, h;

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
			int address = (regionX * s + regionZ) * h + regionY;
			//System.out.println("hashCode == "+address);
			return address;
		}
	}

	public WorldChunksHolder(WorldImplementation world)
	{
		this.world = world;
		//this.chunksData = chunksData;
		h = world.getWorldInfo().getSize().height / 8;
		s = world.getWorldInfo().getSize().sizeInChunks / 8;
	}

	public Iterator<ChunkHolder> getLoadedChunkHolders()
	{
		return chunkHolders.values().iterator();
	}
	
	/**
	 * This method is set to private because it creates an empty ChunkHolder and all ChunkHolders should always contain at least one chunk.
	 * @return
	 */
	public ChunkHolder getChunkHolder(int chunkX, int chunkY, int chunkZ, boolean requestLoadIfAbsent)
	{
		ChunkHolder holder = null;
		ChunkHolderKey key = new ChunkHolderKey(chunkX / 8, chunkY / 8, chunkZ / 8 );
		
		//Lock to avoid any issues with another thread making another holder while we handle this
		worldDataLock.lock();
		holder = chunkHolders.get(key);
		
		//Make a new chunkHolder if we can't find it
		if (requestLoadIfAbsent && holder == null && chunkY < h * 8 && chunkY >= 0)
		{
			//Thread.currentThread().dumpStack();
			holder = new ChunkHolder(world, chunkX / 8, chunkY / 8, chunkZ / 8);
			chunkHolders.putIfAbsent(key, holder);
		}
		worldDataLock.unlock();
		
		return holder;
	}
	
	public void setChunk(CubicChunk c)
	{
		ChunkHolder holder = getChunkHolder(c.chunkX, c.chunkY, c.chunkZ, true);

		if (holder != null)
			holder.set(c.chunkX, c.chunkY, c.chunkZ, c);
		else //destroy it to avoid memory leaks
			if (c != null)
				c.destroy();
	}
	
	public void removeChunk(int chunkX, int chunkY, int chunkZ, boolean saveBeforeRemoving)
	{
		ChunkHolder holder = getChunkHolder(chunkX, chunkY, chunkZ, false);
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
	
	public CubicChunk getChunk(int chunkX, int chunkY, int chunkZ, boolean requestLoadIfAbsent)
	{
		ChunkHolder holder = getChunkHolder(chunkX, chunkY, chunkZ, requestLoadIfAbsent);
		if (holder != null)
		{
			return holder.get(chunkX, chunkY, chunkZ, requestLoadIfAbsent);
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

	public void markChunkDirty(int chunkX, int chunkY, int chunkZ)
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
		if (c != null)
			c.markDirty(true);
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
		Iterator<CubicChunk> i = world.getAllLoadedChunks();
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
		Iterator<CubicChunk> i = world.getAllLoadedChunks();
		while(i.hasNext())
		{
			if(!i.next().isAirChunk())
				c++;
		}
		return c;
	}
}
