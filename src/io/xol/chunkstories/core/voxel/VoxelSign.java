package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.Content;
import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityVoxel;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.voxel.VoxelCustomIcon;
import io.xol.chunkstories.api.voxel.VoxelEntity;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.core.entity.voxel.EntitySign;
import io.xol.chunkstories.renderer.VoxelContext;
import io.xol.chunkstories.voxel.models.VoxelRenderer;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.engine.math.lalgb.vector.dp.Vector3dm;
import io.xol.engine.math.lalgb.vector.sp.Vector2fm;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class VoxelSign extends VoxelEntity implements VoxelCustomIcon
{
	public VoxelSign(Content.Voxels store, int id, String name)
	{
		super(store, id, name);
	}

	@Override
	public boolean handleInteraction(Entity entity, Location voxelLocation, Input input, int voxelData)
	{
		return false;
	}

	@Override
	public EntitySign getVoxelEntity(World world, int worldX, int worldY, int worldZ)
	{
		EntityVoxel ev = super.getVoxelEntity(world, worldX, worldY, worldZ);
		if(!(ev instanceof EntitySign))
				throw new RuntimeException("VoxelEntity representation invariant fail, wrong entity found at " + worldX + ":" + worldY + ":" + worldZ);
		return (EntitySign) ev;
	}

	@Override
	protected EntityVoxel createVoxelEntity(World world, int x, int y, int z)
	{
		return new EntitySign((WorldImplementation) world, x, y, z);
	}
	
	@Override
	public VoxelRenderer getVoxelRenderer(VoxelContext info)
	{
		return super.getVoxelRenderer(info);
	}
		
	@Override
	public int onPlace(World world, int x, int y, int z, int voxelData, Entity entity)
	{
		super.onPlace(world, x, y, z, voxelData, entity);
		
		if(entity != null)
		{
			Vector3dm blockLocation = new Vector3dm(x + 0.5, y, z + 0.5);
			blockLocation.sub(entity.getLocation());
			blockLocation.negate();
			
			Vector2fm direction = new Vector2fm((float)(double)blockLocation.getX(), (float)(double)blockLocation.getZ());
			direction.normalize();
			//System.out.println("x:"+direction.x+"y:"+direction.y);
			
			double asAngle = Math.acos(direction.getY()) / Math.PI * 180;
			asAngle *= -1;
			if(direction.getX() < 0)
				asAngle *= -1;
			
			//asAngle += 180.0;
			
			asAngle %= 360.0;
			asAngle += 360.0;
			asAngle %= 360.0;
			
			//System.out.println(asAngle);
			
			int meta = (int)(16 * asAngle / 360);
			voxelData = VoxelFormat.changeMeta(voxelData, meta);
		}
		
		return voxelData;
	}
	
}