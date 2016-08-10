package io.xol.chunkstories.world.iterators;

import java.util.Iterator;

import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.api.world.chunk.ChunksIterator;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.chunk.ChunkHolder;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Iterates over an entire world and gives all *loaded* chunks of it
 * 
 * @author Hugo
 */
public class WorldChunksIterator implements ChunksIterator
{
	Iterator<ChunkHolder> chIterator;
	ChunkHolder currentChunkHolder;
	ChunksIterator currentChunkHolderIterator;

	public WorldChunksIterator(WorldImplementation world)
	{
		chIterator = world.getChunksHolder().getLoadedChunkHolders();
	}

	@Override
	public boolean hasNext()
	{
		//We always want to ask a non-null, non-empty chunk holder
		while (currentChunkHolder == null || !currentChunkHolderIterator.hasNext())
		{
			if (chIterator.hasNext())
			{
				currentChunkHolder = chIterator.next();
				if (!(currentChunkHolder == null))
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
	public Chunk next()
	{
		//We always want to ask a non-null, non-empty chunk holder
		while (currentChunkHolder == null || !currentChunkHolderIterator.hasNext())
		{
			if (chIterator.hasNext())
			{
				currentChunkHolder = chIterator.next();
				if (!(currentChunkHolder == null))
					currentChunkHolderIterator = currentChunkHolder.iterator();
			}
			//We ran out of chunks holder
			else
				break;
		}
		if (currentChunkHolder != null && currentChunkHolderIterator.hasNext())
			return currentChunkHolderIterator.next();
		return null;
	}

	@Override
	public void remove()
	{
		currentChunkHolderIterator.remove();
	}

}
