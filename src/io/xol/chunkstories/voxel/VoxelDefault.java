package io.xol.chunkstories.voxel;

import io.xol.chunkstories.api.Content;
import io.xol.chunkstories.api.material.Material;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.core.item.ItemVoxel;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.item.ItemTypes;
import io.xol.chunkstories.materials.Materials;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.renderer.VoxelContext;
import io.xol.chunkstories.voxel.models.VoxelModel;
import io.xol.chunkstories.voxel.models.VoxelRenderer;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class VoxelDefault implements Voxel
{
	final protected Content.Voxels store;
	
	final protected int voxelID;
	final protected String voxelName;

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
	Material material;

	/**
	 * Creates a Voxel type, internal to the engine
	 * 
	 * @param id
	 *            Unique voxel ID
	 * @param name
	 *            Voxel internal name for localization
	 */
	public VoxelDefault(Content.Voxels store, int id, String name)
	{
		this.store = store;
		
		this.voxelID = id;
		this.voxelName = name;

		this.material = Materials.getMaterialByName(name);
	}
	
	public Content.Voxels store()
	{
		return store;
	}

	/**
	 * Gets the special voxel model this voxel uses, used for engine's ChunkRenderer
	 * 
	 * @param info
	 *            A BlockRenderInfo object containing information on the voxel surroundings
	 * @return The model used.
	 */
	@Override
	public VoxelRenderer getVoxelRenderer(VoxelContext info)
	{
		return model;
	}

	@Override
	public boolean isVoxelLiquid()
	{
		return liquid;
	}

	@Override
	public boolean isVoxelSolid()
	{
		return solid;
	}

	@Override
	public boolean isVoxelSelectable()
	{
		return voxelID > 0 && !isVoxelLiquid();
	}

	/**
	 * Determines if this Voxel uses a custom VoxelModel
	 * 
	 * @return Whether it does
	 */
	@Override
	public boolean isVoxelUsingCustomRenderer()
	{
		return custom_model;
	}

	@Override
	public boolean isVoxelOpaque()
	{
		return opaque;
	}

	@Override
	public boolean isVoxelOpaqueWithItself()
	{
		return self_opaque;
	}

	@Override
	public boolean isAffectedByWind()
	{
		return affectedByWind;
	}

	/**
	 * Gets the Blocklight level this voxel emmits
	 * 
	 * @param data
	 *            The full 4-byte data related to this voxel ( see {@link VoxelFormat VoxelFormat.class} )
	 * @return The aformentioned light level
	 */
	@Override
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
	@Override
	public VoxelTexture getVoxelTexture(int data, VoxelSides side, VoxelContext info)
	{
		return texture[side.ordinal()];
	}

	@Override
	public int getLightLevelModifier(int dataFrom, int dataTo, VoxelSides side)
	{
		if (this.isVoxelOpaque())
			return -15;
		return shading;
	}

	@Override
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
	@Override
	public boolean isFaceOpaque(VoxelSides side, int data)
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
	@Override
	public CollisionBox[] getTranslatedCollisionBoxes(World world, int x, int y, int z)
	{
		CollisionBox[] boxes = getCollisionBoxes(new VoxelContext(world, x, y, z));
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
	@Override
	public CollisionBox[] getCollisionBoxes(VoxelContext info)
	{
		if (voxelID == 0)
			return new CollisionBox[] {};
		return new CollisionBox[] { new CollisionBox(box) };
	}

	/**
	 * Get the assignated ID for this voxel
	 */
	public int getId()
	{
		return voxelID;
	}

	/**
	 * Returns the internal, non localized name of this voxel
	 */
	public String getName()
	{
		return voxelName;
	}

	public boolean sameKind(Voxel facing)
	{
		return this.voxelID == facing.getId();
	}

	@Override
	public ItemPile[] getItems()
	{
		ItemVoxel itemVoxel = (ItemVoxel) ItemTypes.getItemTypeByName("item_voxel").newItem();
		itemVoxel.voxel = this;

		return new ItemPile[] { new ItemPile(itemVoxel) };
	}

	public boolean equals(Object o)
	{
		//Only the id matters
		if (o instanceof VoxelDefault)
		{
			return (((VoxelDefault) o).getId() == this.getId());
		}
		return false;
	}

	@Override
	public Material getMaterial()
	{
		return material;
	}
}
