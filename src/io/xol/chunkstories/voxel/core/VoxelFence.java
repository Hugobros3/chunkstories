package io.xol.chunkstories.voxel.core;

import io.xol.chunkstories.renderer.BlockRenderInfo;
import io.xol.chunkstories.voxel.Voxel;
import io.xol.chunkstories.voxel.VoxelTypes;
import io.xol.chunkstories.voxel.models.VoxelModel;
import io.xol.chunkstories.voxel.models.VoxelModels;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class VoxelFence extends Voxel
{
	public VoxelFence(int id, String name)
	{
		super(id, name);
	}

	public VoxelModel getVoxelModel(BlockRenderInfo info)
	{
		Voxel vox;
		vox = VoxelTypes.get(info.neightborhood[0]);
		boolean connectLeft = vox.isVoxelSolid() || vox.equals(this);
		vox = VoxelTypes.get(info.neightborhood[1]);
		boolean connectFront = vox.isVoxelSolid() || vox.equals(this);
		vox = VoxelTypes.get(info.neightborhood[2]);
		boolean connectRight = vox.isVoxelSolid() || vox.equals(this);
		vox = VoxelTypes.get(info.neightborhood[3]);
		boolean connectBack = vox.isVoxelSolid() || vox.equals(this);
		
		String type = "default";
		if(connectLeft && connectFront && connectRight && connectBack)
			type = "allDir";
		else if(connectLeft && connectFront && connectRight)
			type = "allButBack";
		else if(connectLeft && connectFront && connectBack)
			type = "allButRight";
		else if(connectLeft && connectBack && connectRight)
			type = "allButFront";
		else if(connectBack && connectFront && connectRight)
			type = "allButLeft";
		else if(connectLeft && connectRight)
			type = "allX";
		else if(connectFront && connectBack)
			type = "allZ";
		else if(connectLeft && connectBack)
			type = "leftBack";
		else if(connectRight && connectBack)
			type = "rightBack";
		else if(connectLeft && connectFront)
			type = "leftFront";
		else if(connectRight && connectFront)
			type = "rightFront";
		else if(connectLeft)
			type = "left";
		else if(connectRight)
			type = "right";
		else if(connectFront)
			type = "front";
		else if(connectBack)
			type = "back";
		
		return VoxelModels.getVoxelModel("wood_fence"+"."+type);
	}
}
