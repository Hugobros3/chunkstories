package io.xol.chunkstories.api.entity.interfaces;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Some entity should not be saved in region files, like player-controlled entities
 */
public interface EntityUnsaveable// extends Entity
{
	public boolean shouldSaveIntoRegion();
}
