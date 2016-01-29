package io.xol.chunkstories.world;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ChunksHolders
{
	World world;
	ChunksData chunksData;

	// private ChunkHolder[] data;
	// private boolean[] dataPresent;

	public ConcurrentHashMap<ChunkHolderKey, ChunkHolder> chunkHolders = new ConcurrentHashMap<ChunkHolderKey, ChunkHolder>();

	final int s, h;

	private class ChunkHolderKey
	{
		public int regionX, regionY, regionZ;

		public ChunkHolderKey(int x, int y, int z)
		{
			regionX = x;
			regionY = y;
			regionZ = z;
		}

		public boolean equals(Object o)
		{
			if(!(o instanceof ChunkHolderKey))
				return false;
			ChunkHolderKey chk = (ChunkHolderKey)o;
			boolean equals = chk.regionX == regionX && chk.regionY == regionY && chk.regionZ == regionZ;
			//System.out.println("checking if chk == !" + equals);
			return equals;
		}
		
		public int hashCode()
		{
			int address = (regionX * s + regionZ) * h + regionY;
			//System.out.println("hashCode == "+address);
			return address;
		}
	}

	public ChunksHolders(World world, ChunksData chunksData)
	{
		this.world = world;
		this.chunksData = chunksData;
		h = world.size.height / 8;
		s = world.size.sizeInChunks / 8;

		// data = new ChunkHolder[h * s * s];
		// dataPresent = new boolean[h * s * s];
	}

	public void setChunk(CubicChunk c)
	{
		ChunkHolder holder = getChunkHolder(c.chunkX, c.chunkY, c.chunkZ, false);
		if (holder != null)
		{
			// dataPresent[getAddress(c.chunkX,c.chunkY,c.chunkZ)] = true;
			holder.set(c.chunkX, c.chunkY, c.chunkZ, c);
			// System.out.println("did set chunk lol dp="+c.dataPointer);
		}
		else
		{
			if (c != null)
				c.destroy();
			// System.out.println("Chunk Holder doesn't exist for " +
			// c.toString());
		}
	}

	int getAddress(int chunkX, int chunkY, int chunkZ)
	{
		// Linearize
		int regionX = chunkX / 8;
		int regionY = chunkY / 8;
		int regionZ = chunkZ / 8;

		int address = (regionX * s + regionZ) * h + regionY;
		return address;
	}

	public ChunkHolder getChunkHolder(int chunkX, int chunkY, int chunkZ, boolean load)
	{
		ChunkHolder holder = null;
		// if (chunkY > world.size.sizeInChunks)
		// return null;
		ChunkHolderKey key = new ChunkHolderKey(chunkX / 8, chunkY / 8, chunkZ / 8 );

		holder = chunkHolders.get(key);
		if (holder == null && chunkY < h * 8 && chunkY >= 0)
		{
			holder = new ChunkHolder(world, chunkX / 8, chunkY / 8, chunkZ / 8, false);
			chunkHolders.putIfAbsent(key, holder);
		}
		return holder;
	}

	public void removeChunk(int chunkX, int chunkY, int chunkZ)
	{
		ChunkHolder holder = getChunkHolder(chunkX, chunkY, chunkZ, false);
		boolean emptyHolder = false;
		if (holder != null)
		{
			emptyHolder = holder.removeChunk(chunkX, chunkY, chunkZ);
		}
		if (emptyHolder)
		{
			holder.destroy();
			chunkHolders.remove(new ChunkHolderKey(chunkX / 8, chunkY / 8, chunkZ / 8 ));
		}
		world.ioHandler.notifyChunkUnload(chunkX, chunkY, chunkZ);
	}

	public CubicChunk getChunk(int chunkX, int chunkY, int chunkZ, boolean load)
	{
		ChunkHolder holder = getChunkHolder(chunkX, chunkY, chunkZ, load);
		if (holder != null)
		{
			return holder.get(chunkX, chunkY, chunkZ, load);
		}
		return null;
	}
	
	public void saveAll()
	{
		Iterator<ChunkHolder> i = chunkHolders.values().iterator();
		ChunkHolder holder;
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
				holder.freeAll();
			}
		}
		chunkHolders.clear();
	}

	public void markChunkDirty(int chunkX, int chunkY, int chunkZ)
	{
		int sic = world.size.sizeInChunks;
		if (chunkX < 0)
			chunkX += sic;
		if (chunkY < 0)
			chunkY += sic;
		if (chunkZ < 0)
			chunkZ += sic;
		chunkX = chunkX % sic;
		if (chunkY >= sic)
			return;
		// chunkY = chunkY % sic;
		chunkZ = chunkZ % sic;
		CubicChunk c = getChunk(chunkX, chunkY, chunkZ, false);
		if (c != null)
			c.markDirty(true);
	}

	public void destroy()
	{
		chunkHolders.clear();
	}
}
