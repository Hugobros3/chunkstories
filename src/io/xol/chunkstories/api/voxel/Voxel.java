package io.xol.chunkstories.api.voxel;

import io.xol.chunkstories.api.Content;
import io.xol.chunkstories.api.item.ItemPile;
import io.xol.chunkstories.api.material.Material;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.core.item.ItemVoxel;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.VoxelContext;
import io.xol.chunkstories.voxel.VoxelTexture;
import io.xol.chunkstories.voxel.models.VoxelRenderer;
import io.xol.engine.math.lalgb.vector.dp.Vector3dm;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class Voxel
{
	final protected VoxelType type;
	final protected Content.Voxels store;
	
	public Voxel(VoxelType type)
	{
		this.type = type;
		this.store = type.store();
	}
	
	/** Contains the information parsed from the .voxels file */
	public final VoxelType getType() {
		return type;
	}
	
	/** Get the assignated ID for this voxel, shortcut to VoxelType */
	public final int getId() {
		return type.getId();
	}

	/** Returns the internal, non localized name of this voxel, shortcut to VoxelType */
	public final String getName() {
		return type.getName();
	}
	
	public final Material getMaterial() {
		return type.getMaterial();
	}

	/** @return The custom rendered used or null if default */
	public VoxelRenderer getVoxelRenderer(VoxelContext info) {
		//By default the 'VoxelRenderer' is just wether or not we set-up a model in the .voxels definitions file
		return type.getVoxelModel();
	}
	
	/** Can this Voxel be selected in creative mode ? (or is it skipped ?) */
	public boolean isVoxelSelectable() {
		//Air is intangible and so is water
		return getId() > 0 && !type.isLiquid();
	}
	
	/**
	 * Gets the Blocklight level this voxel emmits
	 * @param data The full 4-byte data related to this voxel ( see {@link VoxelFormat VoxelFormat.class} )
	 * @return The aformentioned light level
	 */
	public byte getLightLevel(int data) {
		//By default the light output is the one defined in the type, you can change it depending on the provided data
		return type.getEmittingLightLevel();
	}

	/**
	 * Gets the texture for this voxel
	 * @param data The full 4-byte data related to this voxel ( see {@link VoxelFormat VoxelFormat.class} )
	 * @param side The side of the block we want the texture of ( see {@link VoxelSides VoxelSides.class} )
	 * @return
	 */
	public VoxelTexture getVoxelTexture(int data, VoxelSides side, VoxelContext info) {
		//By default we don't care about context, we give the same texture to everyone
		return type.getVoxelTexture(side);
	}

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
	public int getLightLevelModifier(int dataFrom, int dataTo, VoxelSides side) {
		if (getType().isOpaque()) //Opaque voxels destroy all light
			return -15;
		return type.getShadingLightLevel(); //Etc
	}

	//TODO move to renderland and never talk about it again
	/*public default void debugRenderCollision(World world, int x, int y, int z)
	{
		CollisionBox[] tboxes = getTranslatedCollisionBoxes(world, x, y, z);
		if (tboxes != null)
			for (CollisionBox box : tboxes)
				if (getType().isSolid())
					box.debugDraw(1, 0, 0, 1.0f);
				else
					box.debugDraw(1, 1, 0, 0.25f);
	}*/

	/**
	 * Used to fine-tune the culling system, allows for a precise, per-face approach to culling.
	 * @param face The side of the block BEING DREW ( not the one we are asking ), so in fact we have to answer for the opposite face, that is the one that this voxel connects with. To get a reference on the sides conventions, see {@link VoxelSides VoxelSides.class}
	 * @param data The data of the block connected to the one being drew by the face j
	 * @return Whether or not that face occlude a whole face and thus we can discard it
	 */
	public boolean isFaceOpaque(VoxelSides side, int data) {
		return type.isOpaque();
	}

	/**
	 * Get the collision boxes for this object, centered as if the block was in x,y,z
	 * 
	 * @param data
	 *            The full 4-byte data related to this voxel ( see {@link VoxelFormat VoxelFormat.class} )
	 * @return An array of CollisionBox or null.
	 */
	public CollisionBox[] getTranslatedCollisionBoxes(World world, int x, int y, int z) {
		CollisionBox[] boxes = getCollisionBoxes(new VoxelContext(world, x, y, z));
		if (boxes != null)
			for (CollisionBox b : boxes)
				b.translate(x, y, z);
		return boxes;
	}

	/**
	 * Overload of getTranslatedCollisionBoxes with a vector3d
	 */
	public CollisionBox[] getTranslatedCollisionBoxes(World world, Vector3dm position) {
		return getTranslatedCollisionBoxes(world, (int)(double)position.getX(), (int)(double)position.getY(), (int)(double)position.getZ());
	}
	
	/**
	 * Get the collision boxes for this object, centered as if the block was in 0,0,0
	 * 
	 * @param The
	 *            full 4-byte data related to this voxel ( see {@link VoxelFormat VoxelFormat.class} )
	 * @return An array of CollisionBox or null.
	 */
	public CollisionBox[] getCollisionBoxes(VoxelContext info) {
		if (getId() == 0) //Air is collisionless
			return new CollisionBox[] {};
		return new CollisionBox[] { new CollisionBox( type.getCollisionBox()) }; //Return the one box in the definition, if you want more make a customClass
	}

	//TODO is this even used ?
	/** Compares wether two voxels are similar */
	public boolean sameKind(Voxel voxel) {
		return this.getId() == voxel.getId();
	}

	/** @return Returns an array of ItemPiles to use in creative inventory */
	public ItemPile[] getItems() {
		//We spawn a ItemVoxel and set it to reflect this one
		ItemVoxel itemVoxel = (ItemVoxel) this.getType().store().parent().items().getItemTypeByName("item_voxel").newItem();
		itemVoxel.voxel = this;
		return new ItemPile[] { new ItemPile(itemVoxel) };
	}
	
	public Content.Voxels store()
	{
		return store;
	}
}
