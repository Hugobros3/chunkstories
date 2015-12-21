package io.xol.chunkstories.voxel;

import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.BlockRenderInfo;
import io.xol.chunkstories.voxel.models.VoxelModel;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class Voxel
{
	int voxelID = 0;
	protected String voxelName;

	VoxelTexture[] texture = new VoxelTexture[6];

	boolean liquid = false;
	boolean solid = true;
	boolean prop = false;
	VoxelModel model = null;
	boolean opaque = true;
	boolean self_opaque = false;

	boolean hasTopTex = false;
	boolean hasBottomTex = false;

	short lightLevel = 0;
	
	public boolean affectedByWind = false;
	public boolean billboard = false;

	//Vector3f color;

	public Voxel(int id, String name)
	{
		voxelID = id;
		voxelName = name;
	}

	/*
	 * public int getTextureID(int side, int meta) { return
	 * texture[side].legacyId; }
	 */

	public VoxelModel getVoxelModel(BlockRenderInfo info)
	{
		return model;
	}

	public Voxel liquid(boolean b)
	{
		liquid = b;
		return this;
	}

	public Voxel solid(boolean b)
	{
		solid = b;
		return this;
	}

	public Voxel prop(boolean b)
	{
		prop = b;
		return this;
	}

	public Voxel opaque(boolean b)
	{
		opaque = b;
		return this;
	}

	public Voxel selfOpaque(boolean b)
	{
		self_opaque = b;
		return this;
	}

	public Voxel topTex(boolean b)
	{
		hasTopTex = b;
		return this;
	}

	public Voxel botTex(boolean b)
	{
		hasBottomTex = b;
		return this;
	}

	public boolean isVoxelLiquid()
	{
		return liquid;
	}

	public boolean isVoxelSolid()
	{
		return solid;
	}

	public boolean isVoxelSelectable()
	{
		return voxelID > 0 && !isVoxelLiquid();
	}

	public boolean isVoxelProp()
	{
		return prop;
	}

	public boolean isVoxelOpaque()
	{
		return opaque;
	}

	public boolean isVoxelOpaqueWithItself()
	{
		return self_opaque;
	}

	public short getLightLevel(int blockdata)
	{
		return lightLevel;
	}

	public Voxel emitting(int e)
	{
		lightLevel = (short) e;
		return this;
	}

	/*public Vector3f getVoxelColor()
	{
		return color;
	}*/

	public VoxelTexture getVoxelTexture(int side, BlockRenderInfo info) // 0 for top, 1 bot,
															// 2,3,4,5
															// north/south/east/west
	{
		return texture[side];
	}

	public void debugRenderCollision(int x, int y, int z, int data)
	{
		CollisionBox[] tboxes = getTranslatedCollisionBoxes(x, y, z, data);
		if (tboxes != null)
			for (CollisionBox b : tboxes)
				if (this.isVoxelSolid())
					b.debugDraw(1, 0, 0);
				else
					b.debugDraw(1, 1, 0);

	}

	public CollisionBox[] getTranslatedCollisionBoxes(int x, int y, int z,
			int data)
	{
		CollisionBox[] boxes = getCollisionBoxes(data);
		if (boxes != null)
			for (CollisionBox b : boxes)
				b.translate(x, y, z);
		return boxes;
	}

	CollisionBox box;

	public CollisionBox[] getCollisionBoxes(int data)
	{
		if (voxelID == 0)
			return null;
		return new CollisionBox[] { new CollisionBox(box) };
	}
}
