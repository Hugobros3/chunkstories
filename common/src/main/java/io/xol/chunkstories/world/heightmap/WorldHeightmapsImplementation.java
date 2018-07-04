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
import io.xol.chunkstories.api.math.Math2;
import io.xol.chunkstories.api.player.Player;
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
	public HeightmapImplementation aquireHeightmap(WorldUser worldUser, int regionX, int regionZ)
	{
		HeightmapImplementation summary;
		int dirX = 0;
		int dirZ = 0;

		if (worldUser instanceof Player) {
			Player player = (Player)worldUser;

			int playerRegionX = Math2.floor((player.getControlledEntity().getLocation().x()) / 256);
			int playerRegionZ = Math2.floor((player.getControlledEntity().getLocation().z()) / 256);

			if (regionX < playerRegionX)
				dirX = -1;
			if (regionX > playerRegionX)
				dirX = 1;

			if (regionZ < playerRegionZ)
				dirZ = -1;
			if (regionZ > playerRegionZ)
				dirZ = 1;
		}

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
			summary = new HeightmapImplementation(this, regionX, regionZ, dirX, dirZ);
			summaries.put(i, summary);
		}
		dontDeleteWhileCreating.release();
		
		//NOTE: WARNING: ETC: CONTROVERSIAL CHANGE HITLER. 
		//Change of spec. Returns the summary no matter what. No way of knowing if the add was redundant, that's on you.
		return summary.registerUser(worldUser) ? summary : summary;
	}

	@Override
	public HeightmapImplementation aquireHeightmapChunkCoordinates(WorldUser worldUser, int chunkX, int chunkZ)
	{
		chunkX %= worldSizeInChunks;
		chunkZ %= worldSizeInChunks;
		if (chunkX < 0)
			chunkX += worldSizeInChunks;
		if (chunkZ < 0)
			chunkZ += worldSizeInChunks;
		
		return aquireHeightmap(worldUser, chunkX / 8, chunkZ / 8);
	}

	@Override
	public HeightmapImplementation aquireHeightmapWorldCoordinates(WorldUser worldUser, int worldX, int worldZ)
	{
		worldX = sanitizeHorizontalCoordinate(worldX);
		worldZ = sanitizeHorizontalCoordinate(worldZ);
		return aquireHeightmap(worldUser, worldX / 256, worldZ / 256);
	}

	@Override
	public HeightmapImplementation aquireHeightmapLocation(WorldUser worldUser, Location location)
	{
		return aquireHeightmap(worldUser, (int)(double)location.x(), (int)(double)location.z());
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
