package io.xol.chunkstories.api.entity.interfaces;

import io.xol.chunkstories.api.entity.Entity;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Some entity should not be saved in region files, like player-controlled entities
 */
public interface EntityUnsaveable extends Entity
{
	public boolean shouldSaveIntoRegion();
}
