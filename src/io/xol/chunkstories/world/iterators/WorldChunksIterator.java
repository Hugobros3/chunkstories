package io.xol.chunkstories.world.iterators;

import java.util.Iterator;

import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.api.world.chunk.ChunksIterator;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.region.RegionImplementation;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Iterates over an entire world and gives all *loaded* chunks of it
 */
public class WorldChunksIterator implements ChunksIterator
{
	Iterator<RegionImplementation> regionsIterator;
	RegionImplementation currentRegion;
	ChunksIterator currentRegionChunksIterator;

	public WorldChunksIterator(WorldImplementation world)
	{
		regionsIterator = world.getRegionsHolder().getLoadedRegions();
	}

	@Override
	public boolean hasNext()
	{
		//We always want to ask a non-null, non-empty chunk holder
		while (currentRegion == null || !currentRegionChunksIterator.hasNext())
		{
			if (regionsIterator.hasNext())
			{
				currentRegion = regionsIterator.next();
				if (!(currentRegion == null))
					currentRegionChunksIterator = currentRegion.iterator();
			}
			//We ran out of chunks holder
			else
				break;
		}
		//Does it have something for us ?
		return (currentRegion != null && currentRegionChunksIterator.hasNext());
	}

	@Override
	public Chunk next()
	{
		//We always want to ask a non-null, non-empty chunk holder
		while (currentRegion == null || !currentRegionChunksIterator.hasNext())
		{
			if (regionsIterator.hasNext())
			{
				currentRegion = regionsIterator.next();
				if (!(currentRegion == null))
					currentRegionChunksIterator = currentRegion.iterator();
			}
			//We ran out of chunks holder
			else
				break;
		}
		if (currentRegion != null && currentRegionChunksIterator.hasNext())
			return currentRegionChunksIterator.next();
		return null;
	}

	@Override
	public void remove()
	{
		currentRegionChunksIterator.remove();
	}

}
