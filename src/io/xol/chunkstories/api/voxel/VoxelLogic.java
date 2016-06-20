package io.xol.chunkstories.api.voxel;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface VoxelLogic
{
	/**
	 * Called when a voxel implementing this interface is placed
	 * @param voxelMeta For blocks with different subtypes, fi it gives the voxelMeta property of the ItemVoxel
	 * @param entity If placed by an entity
	 */
	public void onPlace(Location loc, short voxelMeta, Entity entity);
	
	/**
	 * Called when a voxel implementing this interface is removed
	 * @param voxelData Complete data of the voxel being removed
	 * @param entity If removed by an entity
	 */
	public void onRemove(Location loc, int voxelData, Entity entity);
}
