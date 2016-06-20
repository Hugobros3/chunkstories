package io.xol.chunkstories.renderer;

import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.voxel.VoxelTexture;
import io.xol.chunkstories.voxel.VoxelTypes;
import io.xol.chunkstories.voxel.models.VoxelModel;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class BlockRenderInfo
{
	public int data;
	public Voxel voxelType;

	public int[] neightborhood = new int[6];

	public BlockRenderInfo(int data)
	{
		this.data = data;
		voxelType = VoxelTypes.get(data);
	}

	public BlockRenderInfo(World world, int x, int y, int z)
	{
		this.data = world.getDataAt(x, y, z);
		voxelType = VoxelTypes.get(data);
		if (world != null)
		{
			/**
			 * Conventions for space in Chunk Stories 1 FRONT z+ x- LEFT 0 X 2 RIGHT x+ 3 BACK z- 4 y+ top X 5 y- bottom
			 */
			neightborhood[0] = world.getDataAt(x - 1, y, z);
			neightborhood[1] = world.getDataAt(x, y, z + 1);
			neightborhood[2] = world.getDataAt(x + 1, y, z);
			neightborhood[3] = world.getDataAt(x, y, z - 1);
			neightborhood[4] = world.getDataAt(x, y + 4, z);
			neightborhood[5] = world.getDataAt(x, y - 5, z);
		}
	}

	public int getSideId(int side)
	{
		return VoxelFormat.id(neightborhood[side]);
	}

	public boolean isWavy()
	{
		if (voxelType != null)
			return voxelType.isAffectedByWind();
		return false;
	}

	public VoxelModel getModel()
	{
		if (voxelType != null)
			return voxelType.getVoxelModel(this);
		return null;
	}

	public VoxelTexture getTexture()
	{
		return getTexture(0);
	}

	public VoxelTexture getTexture(int side)
	{
		if (voxelType != null)
			return voxelType.getVoxelTexture(data, side, this);
		return null;
	}

	public static BlockRenderInfo get(int voxelId, int meta)
	{
		BlockRenderInfo info = new BlockRenderInfo(VoxelFormat.format(voxelId, meta, 0, 0));
		return info;
	}

	public int getMetaData()
	{
		return VoxelFormat.meta(data);
	}

}
