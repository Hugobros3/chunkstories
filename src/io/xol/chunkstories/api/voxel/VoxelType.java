package io.xol.chunkstories.api.voxel;

import io.xol.chunkstories.api.Content;
import io.xol.chunkstories.api.content.NamedWithProperties;
import io.xol.chunkstories.api.material.Material;
import io.xol.chunkstories.api.voxel.models.VoxelModel;
import io.xol.chunkstories.api.voxel.textures.VoxelTexture;
import io.xol.chunkstories.physics.CollisionBox;

public interface VoxelType extends NamedWithProperties
{
	/** Get the assignated ID for this voxel */
	public Content.Voxels store();
	
	/** Get the assignated ID for this voxel */
	public int getId();

	/** Returns the internal, non localized name of this voxel */
	public String getName();
	
	/** Returns the material used by this Voxel */
	public Material getMaterial();

	/** Returns the voxelModel specified in the .voxels file, or null. */
	public VoxelModel getVoxelModel();

	/** Returns the collisionBox defined in the .voxels file, or a default one if none was. */
	public CollisionBox getCollisionBox();
	
	/** Gets the texture for this voxel
	 ** @param side The side of the block we want the texture of ( see {@link VoxelSides VoxelSides.class} ) */
	public VoxelTexture getVoxelTexture(VoxelSides side);

	boolean isSolid();

	boolean isOpaque();
	
	byte getShadingLightLevel();

	byte getEmittingLightLevel();

	boolean isBillboard();

	boolean isLiquid();

	boolean isSelfOpaque();
}
