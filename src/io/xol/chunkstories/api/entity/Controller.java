package io.xol.chunkstories.api.entity;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

/**
 * Implemented both by Client.class and ServerPlayer.class, defines callbacks used for network synchronisation
 * 
 * @author gobrosse
 */
public interface Controller
{
	/**
	 * Called whenever a teleport is issued on the entity
	 * 
	 * @param entity
	 */
	default public void notifyTeleport(Entity entity)
	{
		// Do nothing
	}

	/**
	 * Called whenever the entity's inventory is changed
	 * 
	 * @param entity
	 */
	default public void notifyInventoryChange(Entity entity)
	{

	}
}
