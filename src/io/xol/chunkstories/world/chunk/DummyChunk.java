package io.xol.chunkstories.world.chunk;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.math.vector.dp.Vector3dm;
import io.xol.chunkstories.api.utils.IterableIterator;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.api.world.chunk.Region;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

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
	public void bakeVoxelLightning(boolean considerAdjacentChunks)
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
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public Entity next()
			{
				// TODO Auto-generated method stub
				return null;
			}

		};
	}

	@Override
	public ChunkVoxelContext peek(Vector3dm location)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ChunkVoxelContext peek(int x, int y, int z)
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*@Override
	public ChunkStorage getChunkMeta()
	{
		return new ChunkStorage() {

			@Override
			public Chunk chunk()
			{
				return DummyChunk.this;
			}

			@Override
			public VoxelStorage obtainVoxelStorage(int x, int y, int z, VoxelStorageInitializer vsi)
			{
				return vis == null ? null : vsi.initializeVoxelStorage(peek(x, y, z));
			}
			
		};
	}*/

}
