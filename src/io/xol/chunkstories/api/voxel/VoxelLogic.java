package io.xol.chunkstories.api.voxel;

import io.xol.chunkstories.api.entity.Entity;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * A voxel that has special logic for placement and removal
 */
public interface VoxelLogic
{
	/**
	 * Called when a voxel implementing this interface is placed
	 * @param voxelData The intended data to place at this location
	 * @param entity If placed by an entity
	 * 
	 * @return The modified data to actually place there
	 */
	public int onPlace(int x, int y, int z, int voxelData, Entity entity);
	
	/**
	 * Called when a voxel implementing this interface is removed
	 * @param voxelData Complete data of the voxel being removed
	 * @param entity If removed by an entity
	 */
	public void onRemove(int x, int y, int z, int voxelData, Entity entity);
}
