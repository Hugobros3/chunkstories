package io.xol.chunkstories.api.voxel;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.world.World.WorldVoxelContext;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Describes a Voxel that entities can interact with
 */
public interface VoxelInteractive
{
	/**
	 * 
	 * @param voxelData The {@link VoxelFormat formatted} data
	 * @return True if the interaction was handled and don't need to be spread anymore
	 */
	public boolean handleInteraction(Entity entity, WorldVoxelContext voxelContext, Input input);
}
