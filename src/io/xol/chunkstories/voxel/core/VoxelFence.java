package io.xol.chunkstories.voxel.core;

import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.BlockRenderInfo;
import io.xol.chunkstories.voxel.VoxelDefault;
import io.xol.chunkstories.voxel.VoxelTypes;
import io.xol.chunkstories.voxel.models.VoxelModel;
import io.xol.chunkstories.voxel.models.VoxelModels;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class VoxelFence extends VoxelDefault
{
	public VoxelFence(int id, String name)
	{
		super(id, name);
	}

	public VoxelModel getVoxelModel(BlockRenderInfo info)
	{
		Voxel vox = VoxelTypes.get(info.neightborhood[0]);
		boolean connectLeft = (vox.isVoxelSolid() && vox.isVoxelOpaque()) || vox.equals(this);
		vox = VoxelTypes.get(info.neightborhood[1]);
		boolean connectFront = (vox.isVoxelSolid() && vox.isVoxelOpaque()) || vox.equals(this);
		vox = VoxelTypes.get(info.neightborhood[2]);
		boolean connectRight = (vox.isVoxelSolid() && vox.isVoxelOpaque()) || vox.equals(this);
		vox = VoxelTypes.get(info.neightborhood[3]);
		boolean connectBack = (vox.isVoxelSolid() && vox.isVoxelOpaque()) || vox.equals(this);
		
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
	
	public CollisionBox[] getCollisionBoxes(BlockRenderInfo info)
	{
		// System.out.println("kek");
		CollisionBox[] boxes = new CollisionBox[] { new CollisionBox(0.4, 1.1, 0.4) };

		Voxel vox;
		vox = VoxelTypes.get(info.neightborhood[0]);
		boolean connectLeft = vox.isVoxelSolid() || vox.equals(this);
		vox = VoxelTypes.get(info.neightborhood[1]);
		boolean connectFront = vox.isVoxelSolid() || vox.equals(this);
		vox = VoxelTypes.get(info.neightborhood[2]);
		boolean connectRight = vox.isVoxelSolid() || vox.equals(this);
		vox = VoxelTypes.get(info.neightborhood[3]);
		boolean connectBack = vox.isVoxelSolid() || vox.equals(this);

		if (connectLeft && connectFront && connectRight && connectBack)
		{
			boxes = new CollisionBox[] { new CollisionBox(0.4, 1, 1.0), new CollisionBox(1.0, 1, 0.4) };
		}
		else if (connectLeft && connectFront && connectRight)
			boxes = new CollisionBox[] { new CollisionBox(1.0, 1, 0.4), new CollisionBox(0.4, 1, 0.5).translate(0, 0, 0.25) };
		else if (connectLeft && connectFront && connectBack)
			boxes = new CollisionBox[] { new CollisionBox(0.4, 1, 1.0), new CollisionBox(0.5, 1, 0.4).translate(-0.25, 0, 0) };
		else if (connectLeft && connectBack && connectRight)
			boxes = new CollisionBox[] { new CollisionBox(1.0, 1, 0.4), new CollisionBox(0.4, 1, 0.5).translate(0, 0, -0.25) };
		else if (connectBack && connectFront && connectRight)
			boxes = new CollisionBox[] { new CollisionBox(0.4, 1, 1.0), new CollisionBox(0.5, 1, 0.4).translate(0.25, 0, 0) };
		else if (connectLeft && connectRight)
			boxes = new CollisionBox[] { new CollisionBox(1.0, 1, 0.4) };
		else if (connectFront && connectBack)
			boxes = new CollisionBox[] { new CollisionBox(0.4, 1, 1.0) };
		else if (connectLeft && connectBack)
			boxes = new CollisionBox[] { new CollisionBox(0.7, 1, 0.4).translate(-0.15, 0, 0), new CollisionBox(0.4, 1, 0.7).translate(0, 0, -0.15) };
		else if (connectRight && connectBack)
			boxes = new CollisionBox[] { new CollisionBox(0.7, 1, 0.4).translate(0.15, 0, 0), new CollisionBox(0.4, 1, 0.7).translate(0, 0, -0.15) };
		else if (connectLeft && connectFront)
			boxes = new CollisionBox[] { new CollisionBox(0.7, 1, 0.4).translate(-0.15, 0, 0), new CollisionBox(0.4, 1, 0.7).translate(0, 0, 0.15) };
		else if (connectRight && connectFront)
			boxes = new CollisionBox[] { new CollisionBox(0.70, 1, 0.4).translate(0.15, 0, 0), new CollisionBox(0.4, 1, 0.70).translate(0, 0, 0.15) };
		else if (connectLeft)
			boxes = new CollisionBox[] { new CollisionBox(0.7, 1, 0.4).translate(-0.15, 0, 0) };
		else if (connectRight)
			boxes = new CollisionBox[] { new CollisionBox(0.7, 1, 0.4).translate(0.15, 0, 0) };
		else if (connectFront)
			boxes = new CollisionBox[] { new CollisionBox(0.4, 1, 0.7).translate(0, 0, 0.15) };
		else if (connectBack)
			boxes = new CollisionBox[] { new CollisionBox(0.4, 1, 0.7).translate(0, 0, -0.15) };

		for (CollisionBox box : boxes)
			box.translate(+0.5, -1, +0.5);

		return boxes;
	}
}
