package io.xol.chunkstories.renderer;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.voxel.models.VoxelRenderer;
import io.xol.chunkstories.api.voxel.textures.VoxelTexture;
import io.xol.chunkstories.api.world.VoxelContext;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.voxel.VoxelsStore;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class VoxelContextOlder implements VoxelContext
{
	private int data;
	private Voxel voxelType;
	private int[] neightborhood = new int[6];

	public VoxelContextOlder(int data)
	{
		this.data = data;
		voxelType = VoxelsStore.get().getVoxelById(data);
	}
	
	public VoxelContextOlder(Location location)
	{
		this(location.getWorld(), (int)(double)location.getX(), (int)(double)location.getY(), (int)(double)location.getZ());
	}

	public VoxelContextOlder(World world, int x, int y, int z)
	{
		this.data = world.getVoxelData(x, y, z);
		voxelType = VoxelsStore.get().getVoxelById(data);
		if (world != null)
		{
			/** @see VoxelSides */
			neightborhood[0] = world.getVoxelData(x - 1, y, z);
			neightborhood[1] = world.getVoxelData(x, y, z + 1);
			neightborhood[2] = world.getVoxelData(x + 1, y, z);
			neightborhood[3] = world.getVoxelData(x, y, z - 1);
			neightborhood[4] = world.getVoxelData(x, y + 1, z);
			neightborhood[5] = world.getVoxelData(x, y - 1, z);
		}
	}
	
	/* (non-Javadoc)
	 * @see io.xol.chunkstories.renderer.VoxelContext#getData()
	 */
	@Override
	public int getData()
	{
		return data;
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.renderer.VoxelContext#getVoxel()
	 */
	@Override
	public Voxel getVoxel()
	{
		return voxelType;
	}
	
	/* (non-Javadoc)
	 * @see io.xol.chunkstories.renderer.VoxelContext#getNeightborData(int)
	 */
	@Override
	public int getNeightborData(int side)
	{
		return neightborhood[side];
	}
	
	/* (non-Javadoc)
	 * @see io.xol.chunkstories.renderer.VoxelContext#getSideId(int)
	 */
	@Override
	public int getSideId(int side)
	{
		return VoxelFormat.id(neightborhood[side]);
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.renderer.VoxelContext#getVoxelRenderer()
	 */
	@Override
	public VoxelRenderer getVoxelRenderer()
	{
		if (voxelType != null)
			return voxelType.getVoxelRenderer(this);
		return null;
	}
	
	/* (non-Javadoc)
	 * @see io.xol.chunkstories.renderer.VoxelContext#getTexture(io.xol.chunkstories.api.voxel.VoxelSides)
	 */
	@Override
	public VoxelTexture getTexture(VoxelSides side)
	{
		if (voxelType != null)
			return voxelType.getVoxelTexture(data, side, this);
		return null;
	}

	/* (non-Javadoc)
	 * @see io.xol.chunkstories.renderer.VoxelContext#getMetaData()
	 */
	@Override
	public int getMetaData()
	{
		return VoxelFormat.meta(data);
	}

}
