package io.xol.chunkstories.entity;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

/**
 * Implemented both by Client and server classes, defines callbacks used for network synchronisation
 * @author gobrosse
 *
 */
public interface Controller
{
	/**
	 * Called whenever a teleport is issued on the entity
	 * @param entity
	 */
	default public <CE extends Entity & EntityControllable> void notifyTeleport(CE entity){
		// Do nothing
	}
	
	/**
	 * Called whenever the entity's inventory is changed
	 * @param entity
	 */
	default public <CE extends Entity & EntityControllable> void notifyInventoryChange(CE entity){
		
	}
}
