package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityVoxel;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.voxel.VoxelCustomIcon;
import io.xol.chunkstories.api.voxel.VoxelEntity;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.core.entity.voxel.EntitySign;
import io.xol.chunkstories.renderer.BlockRenderInfo;
import io.xol.chunkstories.voxel.models.VoxelRenderer;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.engine.math.lalgb.Vector2f;
import io.xol.engine.math.lalgb.Vector3d;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class VoxelSign extends VoxelEntity implements VoxelCustomIcon
{
	public VoxelSign(int id, String name)
	{
		super(id, name);
	}

	@Override
	public boolean handleInteraction(Entity entity, Location voxelLocation, Input input, int voxelData)
	{
		return false;
	}

	@Override
	protected EntityVoxel createVoxelEntity(World world, int x, int y, int z)
	{
		return new EntitySign((WorldImplementation) world, x, y, z);
	}
	
	@Override
	public VoxelRenderer getVoxelModel(BlockRenderInfo info)
	{
		return super.getVoxelModel(info);
	}
		
	@Override
	public int onPlace(World world, int x, int y, int z, int voxelData, Entity entity)
	{
		super.onPlace(world, x, y, z, voxelData, entity);
		
		if(entity != null)
		{
			Vector3d blockLocation = new Vector3d(x + 0.5, y, z + 0.5);
			blockLocation.sub(entity.getLocation());
			blockLocation.negate();
			
			Vector2f direction = new Vector2f((float)blockLocation.x, (float)blockLocation.z);
			direction.normalise();
			//System.out.println("x:"+direction.x+"y:"+direction.y);
			
			double asAngle = Math.acos(direction.y) / Math.PI * 180;
			asAngle *= -1;
			if(direction.x < 0)
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