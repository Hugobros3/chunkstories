package io.xol.chunkstories.world.iterators;

import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.api.world.chunk.ChunksIterator;
import io.xol.chunkstories.world.region.RegionImplementation;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Iterates over a Region
 */
public class RegionIterator implements ChunksIterator
{
	RegionImplementation holder;
	
	Chunk chunk;
	int i = 0;
	
	public RegionIterator(RegionImplementation holder)
	{
		this.holder = holder;
	}
	
	@Override
	public boolean hasNext()
	{
		while(i < 8*8*8)
		{
			chunk = holder.getChunk(i/64, (i/8) % 8, i % 8);
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
			chunk = holder.getChunk(i/64, (i/8) % 8, i % 8);
			i++;
		}
		while(chunk == null && i < 8*8*8);
		
		return chunk;
	}
	
	@Override
	public void remove()
	{
		throw new UnsupportedOperationException();
	}

}
