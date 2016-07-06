package io.xol.chunkstories.world.summary;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import io.xol.chunkstories.api.world.heightmap.RegionSummaries;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.engine.math.LoopingMathHelper;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class WorldHeightmapVersion implements RegionSummaries
{
	private final WorldImplementation world;
	
	private final int worldSize;
	private Map<Long, RegionSummary> summaries = new ConcurrentHashMap<Long, RegionSummary>();

	public WorldHeightmapVersion(WorldImplementation w)
	{
		world = w;
		worldSize = world.getSizeInChunks() * 32;
	}

	long index(int x, int z)
	{
		x /= 256;
		z /= 256;
		int s = world.getSizeInChunks() / 8;
		return x * s + z;
	}

	public RegionSummary getRegionSummaryWorldCoordinates(int x, int z)
	{
		x %= worldSize;
		z %= worldSize;
		if (x < 0)
			x += worldSize;
		if (z < 0)
			z += worldSize;

		long i = index(x, z);
		if (summaries.containsKey(i))
			return summaries.get(i);

		RegionSummary cs = new RegionSummary(world, x / 256, z / 256);

		summaries.put(i, cs);
		return cs;
	}

	public int getHeightMipmapped(int x, int z, int level)
	{
		x %= worldSize;
		z %= worldSize;
		if (x < 0)
			x += worldSize;
		if (z < 0)
			z += worldSize;
		RegionSummary cs = getRegionSummaryWorldCoordinates(x, z);
		if (cs == null)
			return 0;
		return cs.getHeightMipmapped(x % 256, z % 256, level);
	}

	public int getHeightAtWorldCoordinates(int x, int z)
	{
		x %= worldSize;
		z %= worldSize;
		if (x < 0)
			x += worldSize;
		if (z < 0)
			z += worldSize;
		RegionSummary cs = getRegionSummaryWorldCoordinates(x, z);
		if (cs == null)
			return 0;
		return cs.getHeight(x % 256, z % 256);
	}

	public int getMinChunkHeightAt(int x, int z)
	{
		x %= worldSize;
		z %= worldSize;
		if (x < 0)
			x += worldSize;
		if (z < 0)
			z += worldSize;
		RegionSummary cs = getRegionSummaryWorldCoordinates(x, z);
		if (cs == null)
			return 0;
		return cs.getMinChunkHeight(x % 256, z % 256);
	}

	public int getIdAt(int x, int z)
	{
		x %= worldSize;
		z %= worldSize;
		if (x < 0)
			x += worldSize;
		if (z < 0)
			z += worldSize;
		RegionSummary cs = getRegionSummaryWorldCoordinates(x, z);
		return cs.getID(x % 256, z % 256);
	}

	public void blockPlaced(int x, int y, int z, int id)
	{
		x %= worldSize;
		z %= worldSize;
		if (x < 0)
			x += worldSize;
		if (z < 0)
			z += worldSize;
		RegionSummary cs = getRegionSummaryWorldCoordinates(x, z);
		cs.set(x % 256, y, z % 256, id);
	}

	public int countSummaries()
	{
		return summaries.size();
	}

	public void saveAll()
	{
		for (RegionSummary cs : summaries.values())
		{
			cs.saveSummary();
		}
	}

	public void clearAll()
	{
		for (RegionSummary cs : summaries.values())
		{
			cs.destroy();
		}
		summaries.clear();
	}

	public void set(int x, int z, int y, int id)
	{
		x %= worldSize;
		z %= worldSize;
		if (x < 0)
			x += worldSize;
		if (z < 0)
			z += worldSize;
		RegionSummary cs = getRegionSummaryWorldCoordinates(x, z);
		cs.forceSet(x % 256, y, z % 256, id);
	}

	public void removeFurther(int pCX, int pCZ, int distanceInChunks)
	{
		int rx = pCX / 8;
		int rz = pCZ / 8;

		int distInRegions = distanceInChunks / 8;
		int s = world.getSizeInChunks() / 8;
		synchronized(summaries)
		{
			Iterator<Entry<Long, RegionSummary>> iterator = summaries.entrySet().iterator();
			while (iterator.hasNext())
			{
				Entry<Long, RegionSummary> entry = iterator.next();
				long l = entry.getKey();
				int lx = (int) (l / s);
				int lz = (int) (l % s);

				int dx = LoopingMathHelper.moduloDistance(rx, lx, s);
				int dz = LoopingMathHelper.moduloDistance(rz, lz, s);
				// System.out.println("Chunk Summary "+lx+":"+lz+" is "+dx+":"+dz+" away from camera max:"+distInRegions+" total:"+summaries.size());
				if (dx > distInRegions || dz > distInRegions)
				{
					summaries.get(l).destroy();
					iterator.remove();
				}
			}
		}
	}

	public Collection<RegionSummary> all()
	{
		return summaries.values();
	}

	public void destroy()
	{
		for(RegionSummary cs : all())
		{
			cs.destroy();
		}
		summaries.clear();
	}
}
