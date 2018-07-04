//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world.heightmap;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.util.concurrency.Fence;
import io.xol.chunkstories.api.world.WorldUser;
import io.xol.chunkstories.api.world.cell.CellData;
import io.xol.chunkstories.api.world.cell.FutureCell;
import io.xol.chunkstories.api.world.heightmap.Heightmap;
import io.xol.chunkstories.api.world.heightmap.WorldHeightmaps;
import io.xol.chunkstories.util.concurrency.CompoundFence;
import io.xol.chunkstories.world.WorldImplementation;

public class WorldHeightmapsImplementation implements WorldHeightmaps
{
	private final WorldImplementation world;
	private final int worldSize;
	private final int worldSizeInChunks;
	private final int worldSizeInRegions;
	
	private Map<Long, HeightmapImplementation> summaries = new ConcurrentHashMap<Long, HeightmapImplementation>();
	protected Semaphore dontDeleteWhileCreating = new Semaphore(1);

	public WorldHeightmapsImplementation(WorldImplementation world)
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
	public HeightmapImplementation acquireHeightmap(WorldUser worldUser, int regionX, int regionZ) {
		HeightmapImplementation heightmap;

		regionX %= worldSizeInRegions;
		regionZ %= worldSizeInRegions;
		if (regionX < 0)
			regionX += worldSizeInRegions;
		if (regionZ < 0)
			regionZ += worldSizeInRegions;

		long index = index(regionX * 256, regionZ * 256);

		dontDeleteWhileCreating.acquireUninterruptibly();
		if (summaries.containsKey(index)) {
			heightmap = summaries.get(index);
			heightmap.registerUser(worldUser);
		} else {
			heightmap = new HeightmapImplementation(this, regionX, regionZ, worldUser);
			summaries.put(index, heightmap);
		}
		dontDeleteWhileCreating.release();

		return heightmap;
	}

	@Override
	public HeightmapImplementation acquireHeightmapChunkCoordinates(WorldUser worldUser, int chunkX, int chunkZ)
	{
		chunkX %= worldSizeInChunks;
		chunkZ %= worldSizeInChunks;
		if (chunkX < 0)
			chunkX += worldSizeInChunks;
		if (chunkZ < 0)
			chunkZ += worldSizeInChunks;
		
		return acquireHeightmap(worldUser, chunkX / 8, chunkZ / 8);
	}

	@Override
	public HeightmapImplementation acquireHeightmapWorldCoordinates(WorldUser worldUser, int worldX, int worldZ)
	{
		worldX = sanitizeHorizontalCoordinate(worldX);
		worldZ = sanitizeHorizontalCoordinate(worldZ);
		return acquireHeightmap(worldUser, worldX / 256, worldZ / 256);
	}

	@Override
	public HeightmapImplementation acquireHeightmapLocation(WorldUser worldUser, Location location)
	{
		return acquireHeightmap(worldUser, (int)(double)location.x(), (int)(double)location.z());
	}
	
	@Override
	public Heightmap getHeightmap(int regionX, int regionZ)
	{
		return getHeightmapWorldCoordinates(regionX * 256, regionZ * 256);
	}

	@Override
	public Heightmap getHeightmapChunkCoordinates(int chunkX, int chunkZ)
	{
		return getHeightmapWorldCoordinates(chunkX * 32, chunkZ * 32);
	}

	@Override
	public Heightmap getHeightmapLocation(Location location)
	{
		return getHeightmapWorldCoordinates((int)(double)location.x(), (int)(double)location.z());
	}

	public HeightmapImplementation getHeightmapWorldCoordinates(int worldX, int worldZ)
	{
		worldX = sanitizeHorizontalCoordinate(worldX);
		worldZ = sanitizeHorizontalCoordinate(worldZ);

		long i = index(worldX, worldZ);
		
		HeightmapImplementation summary = summaries.get(i);
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
		HeightmapImplementation cs = getHeightmapWorldCoordinates(x, z);
		if (cs == null)
			return Heightmap.NO_DATA;
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
		HeightmapImplementation cs = getHeightmapWorldCoordinates(x, z);
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
		HeightmapImplementation cs = getHeightmapWorldCoordinates(x, z);
		if (cs == null)
			return Heightmap.NO_DATA;
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
		HeightmapImplementation cs = getHeightmapWorldCoordinates(x, z);
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
		HeightmapImplementation cs = getHeightmapWorldCoordinates(x, z);
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
		HeightmapImplementation summary = getHeightmapWorldCoordinates(x, z);
		
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
		for (HeightmapImplementation cs : summaries.values())
		{
			allSummariesSaves.add(cs.save());
		}
		
		return allSummariesSaves;
	}

	/*public void setHeightAndId(int x, int z, int y, int id)
	{
		x %= worldSize;
		z %= worldSize;
		if (x < 0)
			x += worldSize;
		if (z < 0)
			z += worldSize;
		HeightmapImplementation cs = getHeightmapWorldCoordinates(x, z);
		cs.setHeightAndId(x % 256, y, z % 256, id);
	}*/
	
	public void destroy()
	{
		for(HeightmapImplementation cs : summaries.values())
		{
			cs.unloadSummary();
		}
		summaries.clear();
	}

	public WorldImplementation getWorld()
	{
		return world;
	}

	boolean removeSummary(HeightmapImplementation regionSummary)
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

	public Collection<HeightmapImplementation> all() {
		return this.summaries.values();
	}
}
