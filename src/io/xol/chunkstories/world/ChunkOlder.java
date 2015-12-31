package io.xol.chunkstories.world;

import java.util.ArrayList;
import java.util.List;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ChunkOlder
{
	World world;
	ChunksData chunksData;

	private ChunkHolder[] data;
	private boolean[] dataPresent;
	final int s, h;

	public ChunkOlder(World world, ChunksData chunksData)
	{
		this.world = world;
		this.chunksData = chunksData;
		h = world.size.height / 8;
		s = world.size.sizeInChunks / 8;
		data = new ChunkHolder[h * s * s];
		dataPresent = new boolean[h * s * s];
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
			//System.out.println("Chunk Holder doesn't exist for " + c.toString());
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
		if (chunkY > world.size.sizeInChunks)
			return null;
		synchronized (data)
		{
			holder = data[getAddress(chunkX, chunkY, chunkZ)];
			if (load && !dataPresent[getAddress(chunkX, chunkY, chunkZ)])
			{
				holder = new ChunkHolder(world, chunkX / 8, chunkY / 8, chunkZ / 8, false);
				data[getAddress(chunkX, chunkY, chunkZ)] = holder;
				dataPresent[getAddress(chunkX, chunkY, chunkZ)] = true;
			}
		}
		return holder;
	}

	public void removeChunk(int chunkX, int chunkY, int chunkZ)
	{
		ChunkHolder holder = getChunkHolder(chunkX, chunkY, chunkZ, false);// data[getAddress(chunkX,
																			// chunkY,
																			// chunkZ)];
		boolean emptyHolder = false;
		if (holder != null)
		{
			emptyHolder = holder.removeChunk(chunkX, chunkY, chunkZ);
		}
		if (emptyHolder)
		{
			// System.out.println("Removing chunk holder...");
			dataPresent[getAddress(chunkX, chunkY, chunkZ)] = false;
			holder.destroy();
			synchronized (data)
			{
				// data[getAddress(chunkX, chunkY, chunkZ)].save();
				data[getAddress(chunkX, chunkY, chunkZ)] = null;
			}
		}
		world.ioHandler.notifyChunkUnload(chunkX, chunkY, chunkZ);
	}

	public CubicChunk getChunk(int chunkX, int chunkY, int chunkZ, boolean load)
	{
		ChunkHolder holder = getChunkHolder(chunkX, chunkY, chunkZ, load);
		if (holder != null)
		{
			// System.out.println("non-null holder");
			return holder.get(chunkX, chunkY, chunkZ, load);
		}
		// System.out.println("null holder"+load);
		return null;
	}

	public List<CubicChunk> getAllLoadedChunks()
	{
		List<CubicChunk> chunks = new ArrayList<CubicChunk>();
		for (ChunkHolder holder : data)
		{
			if (holder != null)
			{
				for (CubicChunk c : holder.getLoadedChunks())
					chunks.add(c);
			}

		}
		return chunks;
	}

	public List<ChunkHolder> getAllLoadedChunksHolders()
	{
		List<ChunkHolder> holders = new ArrayList<ChunkHolder>();
		for(ChunkHolder holder : data)
		{
			if(holder != null)
				holders.add(holder);
		}
		return holders;
	}

	public void saveAll()
	{
		for (ChunkHolder holder : data)
		{
			if (holder != null)
			{
				holder.save();
			}
		}
	}

	public void clearAll()
	{
		for (ChunkHolder holder : data)
		{
			if (holder != null)
			{
				holder.freeAll();

				dataPresent[((holder.regionX * s) + holder.regionY * h) + holder.regionZ] = false;
				synchronized (data)
				{
					// data[getAddress(chunkX, chunkY, chunkZ)].save();
					data[((holder.regionX * s) + holder.regionY * h) + holder.regionZ] = null;
				}
			}
		}
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
			c.markDirty(false);
	}

	public void destroy()
	{
		data = null;
	}
}
