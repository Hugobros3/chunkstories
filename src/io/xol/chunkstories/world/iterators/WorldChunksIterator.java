package io.xol.chunkstories.world.iterators;

import java.util.Iterator;

import io.xol.chunkstories.api.world.ChunksIterator;
import io.xol.chunkstories.world.World;
import io.xol.chunkstories.world.chunk.ChunkHolder;
import io.xol.chunkstories.world.chunk.CubicChunk;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Iterates over an entire world and gives all *loaded* chunks of it
 * @author Hugo
 *
 */
public class WorldChunksIterator implements ChunksIterator
{	
	Iterator<ChunkHolder> chIterator;
	ChunkHolder currentChunkHolder;
	ChunksIterator currentChunkHolderIterator;
	
	public WorldChunksIterator(World world)
	{
		chIterator = world.getChunksHolder().chunkHolders.values().iterator();
	}
	
	@Override
	public boolean hasNext()
	{
		//We always want to ask a non-null, non-empty chunk holder
		while(currentChunkHolder == null || !currentChunkHolderIterator.hasNext())
		{
			if(chIterator.hasNext())
			{
				currentChunkHolder = chIterator.next();
				if(!(currentChunkHolder == null))
					currentChunkHolderIterator = currentChunkHolder.iterator();
			}
			//We ran out of chunks holder
			else
				break;
		}
		//Does it have something for us ?
		return (currentChunkHolder != null && currentChunkHolderIterator.hasNext());
	}

	@Override
	public CubicChunk next()
	{
		//We always want to ask a non-null, non-empty chunk holder
		while(currentChunkHolder == null || !currentChunkHolderIterator.hasNext())
		{
			if(chIterator.hasNext())
			{
				currentChunkHolder = chIterator.next();
				if(!(currentChunkHolder == null))
					currentChunkHolderIterator = currentChunkHolder.iterator();
			}
			//We ran out of chunks holder
			else
				break;
		}
		if(currentChunkHolder != null && currentChunkHolderIterator.hasNext())
			return currentChunkHolderIterator.next();
		return null;
	}
	
	@Override
	public void remove()
	{
		currentChunkHolderIterator.remove();
	}

}
