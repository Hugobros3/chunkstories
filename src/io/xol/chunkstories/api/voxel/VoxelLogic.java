package io.xol.chunkstories.api.voxel;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.exceptions.IllegalBlockModificationException;
import io.xol.chunkstories.api.world.World;

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
	 * @throws IllegalBlockModificationException If we want to prevent it
	 * 
	 * @return The modified data to actually place there
	 */
	public int onPlace(World world, int x, int y, int z, int voxelData, Entity entity) throws IllegalBlockModificationException;
	
	/**
	 * Called when a voxel implementing this interface is removed
	 * @param voxelData Complete data of the voxel being removed
	 * @param entity If removed by an entity
	 * 
	 * @throws IllegalBlockModificationException If we want to prevent it
	 */
	public void onRemove(World world, int x, int y, int z, int voxelData, Entity entity) throws IllegalBlockModificationException;
	
	/**
	 * Called when a voxel implementing this interface is changed ( but the voxel type remains the same )
	 * @param voxelData Complete new data being applied
	 * @param entity If modified by an entity
	 * 
	 * @throws IllegalBlockModificationException If we want to prevent it
	 * 
	 * @return The modified data to actually place there
	 */
	public int onModification(World world, int x, int y, int z, int voxelData, Entity entity) throws IllegalBlockModificationException;
}
