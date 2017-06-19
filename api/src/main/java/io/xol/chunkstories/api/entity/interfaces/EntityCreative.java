package io.xol.chunkstories.api.entity.interfaces;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.components.EntityComponentCreativeMode;
import io.xol.chunkstories.api.events.voxel.VoxelModificationCause;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface EntityCreative extends Entity
{
	public EntityComponentCreativeMode getCreativeModeComponent();
	
	public default boolean isCreativeMode()
	{
		return getCreativeModeComponent().get();
	}

	public default void setCreativeMode(boolean creativeMode)
	{
		getCreativeModeComponent().set(creativeMode);
	}

	public static final VoxelModificationCause CREATIVE_MODE = new VoxelModificationCause() {

		@Override
		public String getName()
		{
			return "Creative Mode";
		}
		
	};
}
