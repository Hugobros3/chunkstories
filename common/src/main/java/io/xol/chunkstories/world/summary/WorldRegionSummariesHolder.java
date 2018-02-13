package io.xol.chunkstories.world.summary;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.util.concurrency.Fence;
import io.xol.chunkstories.api.world.cell.CellData;
import io.xol.chunkstories.api.world.cell.FutureCell;
import io.xol.chunkstories.api.world.chunk.WorldUser;
import io.xol.chunkstories.api.world.heightmap.RegionSummaries;
import io.xol.chunkstories.api.world.heightmap.RegionSummary;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.engine.concurrency.CompoundFence;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class WorldRegionSummariesHolder implements RegionSummaries
{
	private final WorldImplementation world;
	private final int worldSize;
	private final int worldSizeInChunks;
	private final int worldSizeInRegions;
	
	private Map<Long, RegionSummaryImplementation> summaries = new ConcurrentHashMap<Long, RegionSummaryImplementation>();
	protected Semaphore dontDeleteWhileCreating = new Semaphore(1);

	public WorldRegionSummariesHolder(WorldImplementation world)
	{
		this.world = world;
		this.worldSize = world.getSizeInChunks() * 32;
		this.worldSizeInChunks = world.getSizeInChunks();
		this.worldSizeInRegions = world.getSizeInChunks() / 8;
	}

	long index(int x, int z)
	{
		x /= 256;
		z /= 256;
		return x * worldSizeInRegions + z;
	}

	@Override
	public RegionSummaryImplementation aquireRegionSummary(WorldUser worldUser, int regionX, int regionZ)
	{
		RegionSummaryImplementation summary;
		
		regionX %= worldSizeInRegions;
		regionZ %= worldSizeInRegions;
		if (regionX < 0)
			regionX += worldSizeInRegions;
		if (regionZ < 0)
			regionZ += worldSizeInRegions;

		long i = index(regionX * 256, regionZ * 256);
		
		dontDeleteWhileCreating.acquireUninterruptibly();
		if (summaries.containsKey(i))
			summary = summaries.get(i);
		else
		{
			summary = new RegionSummaryImplementation(this, regionX, regionZ);
			summaries.put(i, summary);
		}
		dontDeleteWhileCreating.release();
		
		//NOTE: WARNING: ETC: CONTROVERSIAL CHANGE HITLER. 
		//Change of spec. Returns the summary no matter what. No way of knowing if the add was redundant, that's on you.
		return summary.registerUser(worldUser) ? summary : summary;
	}

	@Override
	public RegionSummaryImplementation aquireRegionSummaryChunkCoordinates(WorldUser worldUser, int chunkX, int chunkZ)
	{
		chunkX %= worldSizeInChunks;
		chunkZ %= worldSizeInChunks;
		if (chunkX < 0)
			chunkX += worldSizeInChunks;
		if (chunkZ < 0)
			chunkZ += worldSizeInChunks;
		
		return aquireRegionSummary(worldUser, chunkX / 8, chunkZ / 8);
	}

	@Override
	public RegionSummaryImplementation aquireRegionSummaryWorldCoordinates(WorldUser worldUser, int worldX, int worldZ)
	{
		worldX = sanitizeHorizontalCoordinate(worldX);
		worldZ = sanitizeHorizontalCoordinate(worldZ);
		return aquireRegionSummary(worldUser, worldX / 256, worldZ / 256);
	}

	@Override
	public RegionSummaryImplementation aquireRegionSummaryLocation(WorldUser worldUser, Location location)
	{
		return aquireRegionSummary(worldUser, (int)(double)location.x(), (int)(double)location.z());
	}
	
	@Override
	public RegionSummary getRegionSummary(int regionX, int regionZ)
	{
		return getRegionSummaryWorldCoordinates(regionX * 256, regionZ * 256);
	}

	@Override
	public RegionSummary getRegionSummaryChunkCoordinates(int chunkX, int chunkZ)
	{
		return getRegionSummaryWorldCoordinates(chunkX * 32, chunkZ * 32);
	}

	@Override
	public RegionSummary getRegionSummaryLocation(Location location)
	{
		return getRegionSummaryWorldCoordinates((int)(double)location.x(), (int)(double)location.z());
	}

	public RegionSummaryImplementation getRegionSummaryWorldCoordinates(int worldX, int worldZ)
	{
		worldX = sanitizeHorizontalCoordinate(worldX);
		worldZ = sanitizeHorizontalCoordinate(worldZ);

		long i = index(worldX, worldZ);
		
		RegionSummaryImplementation summary = summaries.get(i);
		if(summary == null)// || !summary.isLoaded())
			return null;
		else
			return summary;
	}

	public int getHeightMipmapped(int x, int z, int level)
	{
		x %= worldSize;
		z %= worldSize;
		if (x < 0)
			x += worldSize;
		if (z < 0)
			z += worldSize;
		RegionSummaryImplementation cs = getRegionSummaryWorldCoordinates(x, z);
		if (cs == null)
			return 0;
		return cs.getHeightMipmapped(x % 256, z % 256, level);
	}

	public int getDataMipmapped(int x, int z, int level)
	{
		x %= worldSize;
		z %= worldSize;
		if (x < 0)
			x += worldSize;
		if (z < 0)
			z += worldSize;
		RegionSummaryImplementation cs = getRegionSummaryWorldCoordinates(x, z);
		if (cs == null)
			return 0;
		return cs.getDataMipmapped(x % 256, z % 256, level);
	}

	public int getHeightAtWorldCoordinates(int x, int z)
	{
		x %= worldSize;
		z %= worldSize;
		if (x < 0)
			x += worldSize;
		if (z < 0)
			z += worldSize;
		RegionSummaryImplementation cs = getRegionSummaryWorldCoordinates(x, z);
		if (cs == null)
			return 0;
		return cs.getHeight(x % 256, z % 256);
	}

	public int getRawDataAtWorldCoordinates(int x, int z)
	{
		x %= worldSize;
		z %= worldSize;
		if (x < 0)
			x += worldSize;
		if (z < 0)
			z += worldSize;
		RegionSummaryImplementation cs = getRegionSummaryWorldCoordinates(x, z);
		if(cs == null)
			return 0;
		return cs.getRawVoxelData(x % 256, z % 256);
	}
	
	@Override
	public CellData getTopCellAtWorldCoordinates(int x, int z) {
		x %= worldSize;
		z %= worldSize;
		if (x < 0)
			x += worldSize;
		if (z < 0)
			z += worldSize;
		RegionSummaryImplementation cs = getRegionSummaryWorldCoordinates(x, z);
		if(cs == null)
			return null;
		return cs.getTopCell(x, z);
	}

	public void updateOnBlockPlaced(int x, int y, int z, FutureCell future)
	{
		x %= worldSize;
		z %= worldSize;
		if (x < 0)
			x += worldSize;
		if (z < 0)
			z += worldSize;
		RegionSummaryImplementation summary = getRegionSummaryWorldCoordinates(x, z);
		
		if(summary != null)
			summary.updateOnBlockModification(x % 256, y, z % 256, future);
	}

	public int countSummaries()
	{
		return summaries.size();
	}

	public Fence saveAllLoadedSummaries()
	{
		CompoundFence allSummariesSaves = new CompoundFence();
		for (RegionSummaryImplementation cs : summaries.values())
		{
			allSummariesSaves.add(cs.saveSummary());
		}
		
		return allSummariesSaves;
	}

	public void setHeightAndId(int x, int z, int y, int id)
	{
		x %= worldSize;
		z %= worldSize;
		if (x < 0)
			x += worldSize;
		if (z < 0)
			z += worldSize;
		RegionSummaryImplementation cs = getRegionSummaryWorldCoordinates(x, z);
		cs.setHeightAndId(x % 256, y, z % 256, id);
	}
	
	public void destroy()
	{
		for(RegionSummaryImplementation cs : summaries.values())
		{
			cs.unloadSummary();
		}
		summaries.clear();
	}

	WorldImplementation getWorld()
	{
		return world;
	}

	boolean removeSummary(RegionSummaryImplementation regionSummary)
	{
		try {
			dontDeleteWhileCreating.acquireUninterruptibly();
			return summaries.remove(this.index(regionSummary.getRegionX() * 256, regionSummary.getRegionZ() * 256)) != null;
		} finally {
			dontDeleteWhileCreating.release();
		}
	}
	
	private int sanitizeHorizontalCoordinate(int coordinate)
	{
		coordinate = coordinate % (world.getSizeInChunks() * 32);
		if (coordinate < 0)
			coordinate += world.getSizeInChunks() * 32;
		return coordinate;
	}

	public Collection<RegionSummaryImplementation> all() {
		return this.summaries.values();
	}
}
