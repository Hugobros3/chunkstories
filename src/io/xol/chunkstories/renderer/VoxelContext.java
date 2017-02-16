package io.xol.chunkstories.renderer;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.voxel.models.VoxelRenderer;
import io.xol.chunkstories.api.voxel.textures.VoxelTexture;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.voxel.VoxelsStore;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class VoxelContext
{
	public int data;
	public Voxel voxelType;

	public int[] neightborhood = new int[6];

	public VoxelContext(int data)
	{
		this.data = data;
		voxelType = VoxelsStore.get().getVoxelById(data);
	}
	
	public VoxelContext(Location location)
	{
		this(location.getWorld(), (int)(double)location.getX(), (int)(double)location.getY(), (int)(double)location.getZ());
	}

	public VoxelContext(World world, int x, int y, int z)
	{
		this.data = world.getVoxelData(x, y, z);
		voxelType = VoxelsStore.get().getVoxelById(data);
		if (world != null)
		{
			/**
			 * Conventions for space in Chunk Stories 1 FRONT z+ x- LEFT 0 X 2 RIGHT x+ 3 BACK z- 4 y+ top X 5 y- bottom
			 */
			neightborhood[0] = world.getVoxelData(x - 1, y, z);
			neightborhood[1] = world.getVoxelData(x, y, z + 1);
			neightborhood[2] = world.getVoxelData(x + 1, y, z);
			neightborhood[3] = world.getVoxelData(x, y, z - 1);
			neightborhood[4] = world.getVoxelData(x, y + 1, z);
			neightborhood[5] = world.getVoxelData(x, y - 1, z);
		}
	}

	public Voxel getVoxel()
	{
		return voxelType;
	}
	
	public int getSideId(int side)
	{
		return VoxelFormat.id(neightborhood[side]);
	}

	public VoxelRenderer getVoxelRenderer()
	{
		if (voxelType != null)
			return voxelType.getVoxelRenderer(this);
		return null;
	}
	
	public VoxelTexture getTexture(VoxelSides side)
	{
		if (voxelType != null)
			return voxelType.getVoxelTexture(data, side, this);
		return null;
	}

	public static VoxelContext get(int voxelId, int meta)
	{
		VoxelContext info = new VoxelContext(VoxelFormat.format(voxelId, meta, 0, 0));
		return info;
	}

	public int getMetaData()
	{
		return VoxelFormat.meta(data);
	}

}
