package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityVoxel;
import io.xol.chunkstories.api.exceptions.IllegalBlockModificationException;
import io.xol.chunkstories.api.input.Input;

import org.joml.Vector2f;
import org.joml.Vector3d;
import io.xol.chunkstories.api.voxel.VoxelCustomIcon;
import io.xol.chunkstories.api.voxel.VoxelEntity;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelType;
import io.xol.chunkstories.api.voxel.models.VoxelRenderer;
import io.xol.chunkstories.api.world.VoxelContext;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.World.WorldVoxelContext;
import io.xol.chunkstories.core.entity.voxel.EntitySign;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class VoxelSign extends VoxelEntity implements VoxelCustomIcon
{
	
	public VoxelSign(VoxelType type)
	{
		super(type);
	}

	@Override
	public boolean handleInteraction(Entity entity, WorldVoxelContext voxelContext, Input input)
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
		return new EntitySign(store.parent().entities().getEntityTypeByName("sign"), world, x, y, z);
	}
	
	@Override
	public VoxelRenderer getVoxelRenderer(VoxelContext info)
	{
		return super.getVoxelRenderer(info);
	}
		
	@Override
	public int onPlace(World world, int x, int y, int z, int voxelData, Entity entity) throws IllegalBlockModificationException
	{
		super.onPlace(world, x, y, z, voxelData, entity);
		
		if(entity != null)
		{
			Vector3d blockLocation = new Vector3d(x + 0.5, y, z + 0.5);
			blockLocation.sub(entity.getLocation());
			blockLocation.negate();
			
			Vector2f direction = new Vector2f((float)(double)blockLocation.x(), (float)(double)blockLocation.z());
			direction.normalize();
			//System.out.println("x:"+direction.x+"y:"+direction.y);
			
			double asAngle = Math.acos(direction.y()) / Math.PI * 180;
			asAngle *= -1;
			if(direction.x() < 0)
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