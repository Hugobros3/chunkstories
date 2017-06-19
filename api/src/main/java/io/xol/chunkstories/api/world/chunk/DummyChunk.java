package io.xol.chunkstories.api.world.chunk;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.math.vector.dp.Vector3dm;
import io.xol.chunkstories.api.util.IterableIterator;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.world.World;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** A fake chunk, for all the non-reality fans 
 * Also, might be usefull when dealing with blocks all by themselves. */
public class DummyChunk implements Chunk
{
	@Override
	public World getWorld()
	{
		return null;
	}

	@Override
	public Region getRegion()
	{
		return null;
	}

	@Override
	public int getChunkX()
	{
		return 0;
	}

	@Override
	public int getChunkY()
	{
		return 0;
	}

	@Override
	public int getChunkZ()
	{
		return 0;
	}

	@Override
	public int getVoxelData(int x, int y, int z)
	{
		return VoxelFormat.changeSunlight(0, 15);
	}

	@Override
	public void setVoxelDataWithUpdates(int x, int y, int z, int data)
	{
	}

	@Override
	public void setVoxelDataWithoutUpdates(int x, int y, int z, int data)
	{
	}

	@Override
	public void computeVoxelLightning(boolean considerAdjacentChunks)
	{
	}

	@Override
	public boolean needsLightningUpdates()
	{
		return false;
	}

	@Override
	public void markInNeedForLightningUpdate()
	{
	}

	@Override
	public int getSunLight(int x, int y, int z)
	{
		return 15;
	}

	@Override
	public int getBlockLight(int x, int y, int z)
	{
		return 0;
	}

	@Override
	public void setSunLight(int x, int y, int z, int level)
	{
	}

	@Override
	public void setBlockLight(int x, int y, int z, int level)
	{
	}

	@Override
	public boolean isAirChunk()
	{
		return true;
	}

	@Override
	public void destroy()
	{
	}

	@Override
	public IterableIterator<Entity> getEntitiesWithinChunk()
	{
		return new IterableIterator<Entity>()
		{

			@Override
			public boolean hasNext()
			{
				return false;
			}

			@Override
			public Entity next()
			{
				return null;
			}

		};
	}

	@Override
	public ChunkVoxelContext peek(Vector3dm location)
	{
		return null;
	}

	@Override
	public ChunkVoxelContext peek(int x, int y, int z)
	{
		return null;
	}
}
