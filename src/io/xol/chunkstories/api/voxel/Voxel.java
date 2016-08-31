package io.xol.chunkstories.api.voxel;

import io.xol.chunkstories.api.material.Material;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.VoxelContext;
import io.xol.chunkstories.voxel.VoxelTexture;
import io.xol.chunkstories.voxel.models.VoxelRenderer;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.engine.math.lalgb.Vector3d;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface Voxel
{
	/**
	 * Get the assignated ID for this voxel
	 */
	public int getId();

	/**
	 * Returns the internal, non localized name of this voxel
	 */
	public String getName();
	
	public Material getMaterial();

	/** Returns true if this voxel uses a custom VoxelRenderer */
	public boolean isVoxelUsingCustomRenderer();

	/**
	 * @return The custom rendered used or null if default
	 */
	public VoxelRenderer getVoxelRenderer(VoxelContext info);

	public boolean isVoxelLiquid();

	public boolean isVoxelSolid();

	public boolean isVoxelSelectable();

	public boolean isVoxelOpaque();

	public boolean isVoxelOpaqueWithItself();

	/**
	 * Gets the Blocklight level this voxel emmits
	 * @param data The full 4-byte data related to this voxel ( see {@link VoxelFormat VoxelFormat.class} )
	 * @return The aformentioned light level
	 */
	public short getLightLevel(int data);

	/**
	 * Gets the texture for this voxel
	 * @param data The full 4-byte data related to this voxel ( see {@link VoxelFormat VoxelFormat.class} )
	 * @param side The side of the block we want the texture of ( see {@link VoxelSides VoxelSides.class} )
	 * @return
	 */
	public VoxelTexture getVoxelTexture(int data, VoxelSides side, VoxelContext info);

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
	public int getLightLevelModifier(int dataFrom, int dataTo, VoxelSides side);

	public default void debugRenderCollision(World world, int x, int y, int z)
	{
		CollisionBox[] tboxes = getTranslatedCollisionBoxes(world, x, y, z);
		if (tboxes != null)
			for (CollisionBox box : tboxes)
				if (this.isVoxelSolid())
					box.debugDraw(1, 0, 0, 1.0f);
				else
					box.debugDraw(1, 1, 0, 0.25f);
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
	public boolean isFaceOpaque(VoxelSides side, int data);

	/**
	 * Get the collision boxes for this object, centered as if the block was in x,y,z
	 * 
	 * @param data
	 *            The full 4-byte data related to this voxel ( see {@link VoxelFormat VoxelFormat.class} )
	 * @return An array of CollisionBox or null.
	 */
	public default CollisionBox[] getTranslatedCollisionBoxes(World world, int x, int y, int z)
	{
		CollisionBox[] boxes = getCollisionBoxes(new VoxelContext(world, x, y, z));
		if (boxes != null)
			for (CollisionBox b : boxes)
				b.translate(x, y, z);
		return boxes;
	}

	/**
	 * Overload of getTranslatedCollisionBoxes with a vector3d
	 */
	public default CollisionBox[] getTranslatedCollisionBoxes(WorldImplementation world, Vector3d position)
	{
		return getTranslatedCollisionBoxes(world, (int)position.getX(), (int)position.getY(), (int)position.getZ());
	}
	
	/**
	 * Get the collision boxes for this object, centered as if the block was in 0,0,0
	 * 
	 * @param The
	 *            full 4-byte data related to this voxel ( see {@link VoxelFormat VoxelFormat.class} )
	 * @return An array of CollisionBox or null.
	 */
	public CollisionBox[] getCollisionBoxes(VoxelContext info);

	public boolean sameKind(Voxel voxel);

	/**
	 * Defines if this voxel reacts to wind and waves about
	 * 
	 * @return
	 */
	public boolean isAffectedByWind();

	/**
	 * @return Returns an array of ItemPiles to use in creative inventory
	 */
	public ItemPile[] getItems();
}
