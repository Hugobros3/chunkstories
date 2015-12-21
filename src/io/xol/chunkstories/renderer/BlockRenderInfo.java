package io.xol.chunkstories.renderer;

import io.xol.chunkstories.voxel.Voxel;
import io.xol.chunkstories.voxel.VoxelFormat;
import io.xol.chunkstories.voxel.VoxelTexture;
import io.xol.chunkstories.voxel.models.VoxelModel;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class BlockRenderInfo
{
	//Stores data next to this tile :
	
	//           1    FRONT z+
	// x- LEFT 0 X 2  RIGHT x+
	//           3    BACK  z-
	// 4 y+ top
	// X
	// 5 y- bottom
	
	public int data;
	public Voxel voxelType;
	
	public int[] neightborhood = new int[6];
	
	public int getSideId(int side)
	{
		return VoxelFormat.id(neightborhood[side]);
	}
	
	public static enum Sides {
		LEFT,
		FRONT,
		RIGHT,
		BACK,
		TOP,
		BOTTOM;
	}
	
	public boolean isWavy()
	{
		if(voxelType != null)
			return voxelType.affectedByWind;
		return false;
	}

	public VoxelModel getModel()
	{
		if(voxelType != null)
			return voxelType.getVoxelModel(this);
		return null;
	}
	
	public VoxelTexture getTexture()
	{
		return getTexture(0);
	}

	public VoxelTexture getTexture(int side)
	{
		if(voxelType != null)
			return voxelType.getVoxelTexture(side, this);
		return null;
	}

	public static BlockRenderInfo get(int voxelId, int meta)
	{
		BlockRenderInfo info = new BlockRenderInfo();
		info.data = VoxelFormat.format(meta, meta, 0, 0);
		return info;
	}

	public int getMetaData()
	{
		return VoxelFormat.meta(data);
	}
	
}
