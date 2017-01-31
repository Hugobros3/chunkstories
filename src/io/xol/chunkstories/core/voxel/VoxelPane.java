package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.VoxelContext;
import io.xol.chunkstories.api.Content;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.voxel.VoxelDefault;
import io.xol.chunkstories.voxel.VoxelsStore;
import io.xol.chunkstories.voxel.models.VoxelModel;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class VoxelPane extends VoxelDefault
{
	public VoxelPane(Content.Voxels store, int id, String name)
	{
		super(store, id, name);
	}

	@Override
	public VoxelModel getVoxelRenderer(VoxelContext info)
	{
		Voxel vox;
		vox = VoxelsStore.get().getVoxelById(info.neightborhood[0]);
		boolean connectLeft = vox.isVoxelSolid() || vox.equals(this);
		vox = VoxelsStore.get().getVoxelById(info.neightborhood[1]);
		boolean connectFront = vox.isVoxelSolid() || vox.equals(this);
		vox = VoxelsStore.get().getVoxelById(info.neightborhood[2]);
		boolean connectRight = vox.isVoxelSolid() || vox.equals(this);
		vox = VoxelsStore.get().getVoxelById(info.neightborhood[3]);
		boolean connectBack = vox.isVoxelSolid() || vox.equals(this);

		String type = "default";
		if (connectLeft && connectFront && connectRight && connectBack)
			type = "allDir";
		else if (connectLeft && connectFront && connectRight)
			type = "allButBack";
		else if (connectLeft && connectFront && connectBack)
			type = "allButRight";
		else if (connectLeft && connectBack && connectRight)
			type = "allButFront";
		else if (connectBack && connectFront && connectRight)
			type = "allButLeft";
		else if (connectLeft && connectRight)
			type = "allX";
		else if (connectFront && connectBack)
			type = "allZ";
		else if (connectLeft && connectBack)
			type = "leftBack";
		else if (connectRight && connectBack)
			type = "rightBack";
		else if (connectLeft && connectFront)
			type = "leftFront";
		else if (connectRight && connectFront)
			type = "rightFront";
		else if (connectLeft)
			type = "left";
		else if (connectRight)
			type = "right";
		else if (connectFront)
			type = "front";
		else if (connectBack)
			type = "back";
		else
			type = "allDir";

		return store.models().getVoxelModelByName("pane" + "." + type);
	}

	@Override
	public CollisionBox[] getCollisionBoxes(VoxelContext info)
	{
		// System.out.println("kek");
		CollisionBox[] boxes = null;

		Voxel vox;
		vox = VoxelsStore.get().getVoxelById(info.neightborhood[0]);
		boolean connectLeft = vox.isVoxelSolid() || vox.equals(this);
		vox = VoxelsStore.get().getVoxelById(info.neightborhood[1]);
		boolean connectFront = vox.isVoxelSolid() || vox.equals(this);
		vox = VoxelsStore.get().getVoxelById(info.neightborhood[2]);
		boolean connectRight = vox.isVoxelSolid() || vox.equals(this);
		vox = VoxelsStore.get().getVoxelById(info.neightborhood[3]);
		boolean connectBack = vox.isVoxelSolid() || vox.equals(this);

		if (connectLeft && connectFront && connectRight && connectBack)
		{
			boxes = new CollisionBox[] { new CollisionBox(0.45, 0.0, 0.0, 0.1, 1, 1.0), new CollisionBox(0.0, 0.0, 0.45, 1.0, 1, 0.1) };
		}
		else if (connectLeft && connectFront && connectRight)
			boxes = new CollisionBox[] { new CollisionBox(0.0, 0.0, 0.45, 1.0, 1, 0.1), new CollisionBox(0.1, 1, 0.5).translate(0.45, 0, 0.5) };
		else if (connectLeft && connectFront && connectBack)
			boxes = new CollisionBox[] { new CollisionBox(0.45, 0.0, 0.0, 0.1, 1, 1.0), new CollisionBox(0.5, 1, 0.1).translate(0.0, 0, 0.45) };
		else if (connectLeft && connectBack && connectRight)
			boxes = new CollisionBox[] { new CollisionBox(0.0, 0.0, 0.45, 1.0, 1, 0.1), new CollisionBox(0.1, 1, 0.5).translate(0.45, 0, 0.0) };
		else if (connectBack && connectFront && connectRight)
			boxes = new CollisionBox[] { new CollisionBox(0.45, 0.0, 0.0, 0.1, 1, 1.0), new CollisionBox(0.5, 1, 0.1).translate(0.5, 0, 0.45) };
		else if (connectLeft && connectRight)
			boxes = new CollisionBox[] { new CollisionBox(0.0, 0.0, 0.45, 1.0, 1, 0.1) };
		else if (connectFront && connectBack)
			boxes = new CollisionBox[] { new CollisionBox(0.45, 0.0, 0.0, 0.1, 1, 1.0) };
		else if (connectLeft && connectBack)
			boxes = new CollisionBox[] { new CollisionBox(0.55, 1, 0.1).translate(0.0, 0, 0.45), new CollisionBox(0.1, 1, 0.55).translate(0.45, 0, 0.0) };
		else if (connectRight && connectBack)
			boxes = new CollisionBox[] { new CollisionBox(0.55, 1, 0.1).translate(0.45, 0, 0.0), new CollisionBox(0.1, 1, 0.55).translate(0, 0, 0.45) };
		else if (connectLeft && connectFront)
			boxes = new CollisionBox[] { new CollisionBox(0.55, 1, 0.1).translate(0, 0, 0.45), new CollisionBox(0.1, 1, 0.55).translate(0.45, 0, 0.45) };
		else if (connectRight && connectFront)
			boxes = new CollisionBox[] { new CollisionBox(0.55, 1, 0.1).translate(0.45, 0, 0.45), new CollisionBox(0.1, 1, 0.55).translate(0.45, 0, 0.45) };
		else if (connectLeft)
			boxes = new CollisionBox[] { new CollisionBox(0.0, 0.0, 0.45, 0.5, 1, 0.1).translate(0, 0, 0) };
		else if (connectRight)
			boxes = new CollisionBox[] { new CollisionBox(0.0, 0.0, 0.45, 0.5, 1, 0.1).translate(0.5, 0, 0) };
		else if (connectFront)
			boxes = new CollisionBox[] { new CollisionBox(0.45, 0.0, 0.0, 0.1, 1, 0.5).translate(0, 0, 0.5) };
		else if (connectBack)
			boxes = new CollisionBox[] { new CollisionBox(0.45, 0.0, 0.0, 0.1, 1, 0.5).translate(0, 0, 0.0) };
		else
			boxes = new CollisionBox[] { new CollisionBox(0.45, 0.0, 0.0, 0.1, 1, 1.0), new CollisionBox(0.0, 0.0, 0.45, 1.0, 1, 0.1) };
		
		//for (CollisionBox box : boxes)
		//	box.translate(+0.5, -0, +0.5);

		return boxes;
	}
}
