package io.xol.chunkstories.voxel;

import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.world.WorldInterface;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.BlockRenderInfo;
import io.xol.chunkstories.voxel.models.VoxelModel;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class VoxelDefault extends Voxel
{
	VoxelTexture[] texture = new VoxelTexture[6];

	boolean liquid = false;
	boolean solid = true;
	boolean custom_model = false;
	VoxelModel model = null;
	boolean opaque = true;
	boolean self_opaque = false;

	short lightLevel = 0;
	// How much light is lost when it goes throught ?
	short shading = 0;

	public boolean affectedByWind = false;
	public boolean billboard = false;

	CollisionBox box;

	/**
	 * Creates a Voxel type, internal to the engine
	 * 
	 * @param id
	 *            Unique voxel ID
	 * @param name
	 *            Voxel internal name for localization
	 */
	public VoxelDefault(int id, String name)
	{
		voxelID = id;
		voxelName = name;
	}

	/**
	 * Gets the special voxel model this voxel uses, used for engine's ChunkRenderer
	 * 
	 * @param info
	 *            A BlockRenderInfo object containing information on the voxel surroundings
	 * @return The model used.
	 */
	public VoxelModel getVoxelModel(BlockRenderInfo info)
	{
		return model;
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

	/**
	 * Determines if this Voxel uses a custom VoxelModel
	 * 
	 * @return Whether it does
	 */
	public boolean isVoxelUsingCustomModel()
	{
		return custom_model;
	}

	public boolean isVoxelOpaque()
	{
		return opaque;
	}

	public boolean isVoxelOpaqueWithItself()
	{
		return self_opaque;
	}

	/**
	 * Gets the Blocklight level this voxel emmits
	 * 
	 * @param data
	 *            The full 4-byte data related to this voxel ( see {@link VoxelFormat VoxelFormat.class} )
	 * @return The aformentioned light level
	 */
	public short getLightLevel(int data)
	{
		return lightLevel;
	}

	/**
	 * Gets the texture for this voxel
	 * 
	 * @param data
	 *            The full 4-byte data related to this voxel ( see {@link VoxelFormat VoxelFormat.class} )
	 * @param side
	 *            The side of the block we want the texture of ( see {@link VoxelSides VoxelSides.class} )
	 * @param info
	 * @return
	 */
	public VoxelTexture getVoxelTexture(int data, int side, BlockRenderInfo info)
	{
		return texture[side];
	}

	public int getLightLevelModifier(int dataFrom, int dataTo, int side)
	{
		if (this.isVoxelOpaque())
			return -15;
		return shading;
	}

	public void debugRenderCollision(WorldInterface world, int x, int y, int z)
	{
		CollisionBox[] tboxes = getTranslatedCollisionBoxes(world, x, y, z);
		if (tboxes != null)
			for (CollisionBox b : tboxes)
				if (this.isVoxelSolid())
					b.debugDraw(1, 0, 0, 1.0f);
				else
					b.debugDraw(1, 1, 0, 0.25f);
	}

	/**
	 * Used to fine-tune the culling system, allows for a precise, per-face approach to culling.
	 * 
	 * @param face
	 *            The side of the block BEING DREW ( not the one we are asking ), so in fact we have to answer for the opposite face, that is the one that this voxel connects with. To get a reference on the sides conventions, see {@link VoxelSides VoxelSides.class}
	 * @param data
	 *            The data of the block connected to the one being drew by the face j
	 * @return Whether or not that face occlude a whole face and thus we can discard it
	 */
	public boolean isFaceOpaque(int side, int data)
	{
		return isVoxelOpaque();
	}

	/**
	 * Get the collision boxes for this object, centered as if the block was in x,y,z
	 * 
	 * @param data
	 *            The full 4-byte data related to this voxel ( see {@link VoxelFormat VoxelFormat.class} )
	 * @return An array of CollisionBox or null.
	 */
	public CollisionBox[] getTranslatedCollisionBoxes(WorldInterface world, int x, int y, int z)
	{
		CollisionBox[] boxes = getCollisionBoxes(new BlockRenderInfo(world, x, y, z));
		if (boxes != null)
			for (CollisionBox b : boxes)
				b.translate(x, y, z);
		return boxes;
	}

	/**
	 * Get the collision boxes for this object, centered as if the block was in 0,0,0
	 * 
	 * @param The
	 *            full 4-byte data related to this voxel ( see {@link VoxelFormat VoxelFormat.class} )
	 * @return An array of CollisionBox or null.
	 */
	public CollisionBox[] getCollisionBoxes(BlockRenderInfo info)
	{
		if (voxelID == 0)
			return new CollisionBox[] {};
		return new CollisionBox[] { new CollisionBox(box) };
	}

	@Override
	public boolean isAffectedByWind()
	{
		return affectedByWind;
	}

	@Override
	public ItemPile[] getItems()
	{
		return new ItemPile[] {

		new ItemPile("item_voxel", new String[] { "" + this.voxelID }).duplicate() };
	}
}
