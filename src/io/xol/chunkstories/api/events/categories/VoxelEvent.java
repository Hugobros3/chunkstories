package io.xol.chunkstories.api.events.categories;

import io.xol.chunkstories.api.Location;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Describes an event triggered, centered arround or related to a voxel.
 * @author Hugo
 *
 */
public interface VoxelEvent
{
	public Location getVoxelLocation();
	
	public int getVoxelData();
}
