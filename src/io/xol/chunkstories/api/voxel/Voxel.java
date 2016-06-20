package io.xol.chunkstories.api.voxel;

import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.BlockRenderInfo;
import io.xol.chunkstories.voxel.VoxelTexture;
import io.xol.chunkstories.voxel.models.VoxelModel;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.engine.math.lalgb.Vector3d;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class Voxel
{
	protected int voxelID = 0;
	protected String voxelName;

	/**
	 * Determines if this Voxel uses a custom VoxelModel
	 * 
	 * @return Whether it does
	 */
	public abstract boolean isVoxelUsingCustomModel();

	/**
	 * Gets the special voxel model this voxel uses, used for engine's ChunkRenderer
	 * 
	 * @param info
	 *            A BlockRenderInfo object containing information on the voxel surroundings
	 * @return The model used or null if none
	 */
	public abstract VoxelModel getVoxelModel(BlockRenderInfo info);

	public abstract boolean isVoxelLiquid();

	public abstract boolean isVoxelSolid();

	public abstract boolean isVoxelSelectable();

	public abstract boolean isVoxelOpaque();

	public abstract boolean isVoxelOpaqueWithItself();

	/**
	 * Gets the Blocklight level this voxel emmits
	 * 
	 * @param data
	 *            The full 4-byte data related to this voxel ( see {@link VoxelFormat VoxelFormat.class} )
	 * @return The aformentioned light level
	 */
	public abstract short getLightLevel(int data);

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
	public abstract VoxelTexture getVoxelTexture(int data, int side, BlockRenderInfo info);

	/**
	 * Gets the reduction of the light that will transfer from this block to another, based on data from the two blocks and the side from wich it's leaving the first block from.
	 * 
	 * @param dataFrom
	 *            The full 4-byte data related to this voxel ( see {@link VoxelFormat VoxelFormat.class} )
	 * @param dataTo
	 *            The full 4-byte data related to this voxel ( see {@link VoxelFormat VoxelFormat.class} )
	 * @param side
	 *            The side of the block light would come out of ( see {@link VoxelSides VoxelSides.class} )
	 * @return The reduction to apply to the light level on exit
	 */
	public abstract int getLightLevelModifier(int dataFrom, int dataTo, int side);

	public void debugRenderCollision(World world, int x, int y, int z)
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
	public abstract boolean isFaceOpaque(int side, int data);

	/**
	 * Get the collision boxes for this object, centered as if the block was in x,y,z
	 * 
	 * @param data
	 *            The full 4-byte data related to this voxel ( see {@link VoxelFormat VoxelFormat.class} )
	 * @return An array of CollisionBox or null.
	 */
	public CollisionBox[] getTranslatedCollisionBoxes(World world, int x, int y, int z)
	{
		CollisionBox[] boxes = getCollisionBoxes(new BlockRenderInfo(world, x, y, z));
		if (boxes != null)
			for (CollisionBox b : boxes)
				b.translate(x, y, z);
		return boxes;
	}

	/**
	 * Overload of getTranslatedCollisionBoxes with a vector3d
	 */
	public CollisionBox[] getTranslatedCollisionBoxes(WorldImplementation world, Vector3d position)
	{
		return getTranslatedCollisionBoxes(world, (int)position.x, (int)position.y, (int)position.z);
	}
	
	/**
	 * Get the collision boxes for this object, centered as if the block was in 0,0,0
	 * 
	 * @param The
	 *            full 4-byte data related to this voxel ( see {@link VoxelFormat VoxelFormat.class} )
	 * @return An array of CollisionBox or null.
	 */
	public abstract CollisionBox[] getCollisionBoxes(BlockRenderInfo info);

	/**
	 * Get the assignated ID for this voxel
	 * 
	 * @return etc
	 */
	public int getId()
	{
		return voxelID;
	}

	/**
	 * Returns the internal, non localized name of this voxel
	 * 
	 * @return
	 */
	public String getName()
	{
		return voxelName;
	}

	public boolean sameKind(Voxel facing)
	{
		return this.voxelID == facing.voxelID;
	}

	/**
	 * Defines if this voxel reacts to wind and waves about
	 * 
	 * @return
	 */
	public abstract boolean isAffectedByWind();

	public abstract ItemPile[] getItems();
}
