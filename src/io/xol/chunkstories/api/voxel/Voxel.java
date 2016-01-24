package io.xol.chunkstories.api.voxel;

import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.BlockRenderInfo;
import io.xol.chunkstories.voxel.VoxelTexture;
import io.xol.chunkstories.voxel.models.VoxelModel;
import io.xol.chunkstories.world.World;

public abstract class Voxel
{
	protected int voxelID = 0;
	protected String voxelName;

	/**
	 * Determines if this Voxel uses a custom VoxelModel
	 * @return Whether it does
	 */
	public abstract boolean isVoxelUsingCustomModel();
	
	/**
	 * Gets the special voxel model this voxel uses, used for engine's ChunkRenderer
	 * @param info A BlockRenderInfo object containing information on the voxel surroundings
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
	 * @param data The full 4-byte data related to this voxel ( see {@link VoxelFormat VoxelFormat.class} )
	 * @return The aformentioned light level
	 */
	public abstract short getLightLevel(int data);

	/**
	 * Gets the texture for this voxel
	 * @param data The full 4-byte data related to this voxel ( see {@link VoxelFormat VoxelFormat.class} )
	 * @param side The side of the block we want the texture of ( see {@link VoxelSides VoxelSides.class} )
	 * @param info
	 * @return
	 */
	public abstract VoxelTexture getVoxelTexture(int data, int side, BlockRenderInfo info);
	
	/**
	 * Gets the light level that will exit this block, based on the block information and side.
	 * @param data  The full 4-byte data related to this voxel ( see {@link VoxelFormat VoxelFormat.class} )
	 * @param llIn The 0-15 light level this block is currently at ( either sun or block light )
	 * @param side The side of the block light would come out of ( see {@link VoxelSides VoxelSides.class} )
	 * @return The light 0-15 light level this block emits
	 */
	
	public abstract int getLightLevelOut(int data, int llIn, int side);

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
	 * @param face The side of the block BEING DREW ( not the one we are asking ), so in fact we have to answer for the opposite face, that is the one
	 * that this voxel connects with. To get a reference on the sides conventions, see {@link VoxelSides VoxelSides.class}
	 * @param data The data of the block connected to the one being drew by the face j
	 * @return Whether or not that face occlude a whole face and thus we can discard it
	 */
	public abstract boolean isFaceOpaque(int side, int data);
	
	/**
	 * Get the collision boxes for this object, centered as if the block was in x,y,z
	 * @param data  The full 4-byte data related to this voxel ( see {@link VoxelFormat VoxelFormat.class} )
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
	 * Get the collision boxes for this object, centered as if the block was in 0,0,0
	 * @param  The full 4-byte data related to this voxel ( see {@link VoxelFormat VoxelFormat.class} )
	 * @return An array of CollisionBox or null.
	 */
	public abstract CollisionBox[] getCollisionBoxes(BlockRenderInfo info);

	/**
	 * Get the assignated ID for this voxel
	 * @return etc
	 */
	public int getId()
	{
		return voxelID;
	}

	/**
	 * Returns the internal, non localized name of this voxel
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
	 * @return
	 */
	public abstract boolean isAffectedByWind();
}
