package io.xol.chunkstories.api.voxel;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityVoxel;
import io.xol.chunkstories.api.exceptions.IllegalBlockModificationException;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.api.world.chunk.Region;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * A VoxelEntity is linked to an EntityVoxel, the latter is spawned and destroyed by the voxel, it never moves nor interact and only serves the voxel's interests
 */
public abstract class VoxelEntity extends Voxel implements VoxelLogic, VoxelInteractive
{
	public VoxelEntity(VoxelType type)
	{
		super(type);
	}
	
	public Entity getVoxelEntity(Location location)
	{
		return getVoxelEntity(location.getWorld(), (int)(double)location.getX(), (int)(double)location.getY(), (int)(double)location.getZ());
	}
	
	public EntityVoxel getVoxelEntity(World world, int worldX, int worldY, int worldZ)
	{
		Region region = world.getRegionWorldCoordinates(worldX, worldY, worldZ);
		if (region != null && region.isDiskDataLoaded())
		{
			EntityVoxel entity = region.getEntityVoxelAt(worldX, worldY, worldZ);
			if (entity == null)
				throw new RuntimeException("VoxelEntity representation invariant fail, no entity found at " + worldX + ":" + worldY + ":" + worldZ);
			
			return entity;
		}
		System.out.println("Edge case : looking for an voxel entity in a region that is not supposed to be loaded");
		return null;
	}
	
	protected abstract EntityVoxel createVoxelEntity(World world, int x, int y, int z);

	@Override
	public int onPlace(World world, int x, int y, int z, int voxelData, Entity entity)
	{
		if(!(world instanceof WorldMaster))
			return voxelData;
		
		EntityVoxel voxelEntity = createVoxelEntity(world, x, y, z);

		//System.out.println("added voxel entity");
		
		world.addEntity(voxelEntity);
		
		return voxelData;
	}

	@Override
	public void onRemove(World world, int x, int y, int z, int voxelData, Entity entity)
	{
		if(!(world instanceof WorldMaster))
			return;
		
		Entity voxelEntity = getVoxelEntity(world, x, y, z);
		world.removeEntity(voxelEntity);
		
		//System.out.println("removed voxel entity");
	}

	@Override
	public int onModification(World world, int x, int y, int z, int voxelData, Entity entity) throws IllegalBlockModificationException
	{
		if(entity != null)
			throw new IllegalBlockModificationException("Entities can't fiddle with "+this.getClass().getSimpleName());
		return voxelData;
	}

}
