package io.xol.chunkstories.world.iterators;

import io.xol.chunkstories.api.world.Chunk;
import io.xol.chunkstories.api.world.ChunksIterator;
import io.xol.chunkstories.world.chunk.ChunkHolder;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Iterates over a ChunkHolder
 */
public class ChunkHolderIterator implements ChunksIterator
{
	ChunkHolder holder;
	
	Chunk chunk;
	int i = 0;
	
	public ChunkHolderIterator(ChunkHolder holder)
	{
		this.holder = holder;
		//System.out.println("holder:"+" "+holder);
	}
	
	@Override
	public boolean hasNext()
	{
		while(i < 8*8*8)
		{
			chunk = holder.getChunk(i/64, (i/8) % 8, i % 8, false);
			//If there is a chunk waiting then we return yes
			if(chunk != null)
				return true;
			//If not keep looking
			i++;
		}
		//End of chunk
		return false;
	}

	@Override
	public Chunk next()
	{
		//Returns a chunk and increment by one
		do
		{
			chunk = holder.getChunk(i/64, (i/8) % 8, i % 8, false);
			i++;
		}
		while(chunk == null && i < 8*8*8);
		
		return chunk;
	}
	
	@Override
	public void remove()
	{
		if(chunk == null)
			return;
		holder.removeChunk(chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ());
	}

}
