package io.xol.chunkstories.api.entity;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.entity.core.components.EntityComponentController;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

/**
 * Defines an entity as controllable by players, this is used to allow the Client to send controls to the entity and the
 * server to update the client about it's entity status.
 */
public interface EntityControllable extends Entity
{
	public EntityComponentController getControllerComponent();
	
	public void moveCamera(ClientController controller);
	
	/**
	 * Clientside controller tick, called before
	 */
	public void tick(ClientController controller);
	
	/**
	 * If this entity has the ability to select blocks, this method should return said block
	 * @param inside If set to true it will return the selected block, if set to false it will return the one adjacent to
	 * @return
	 */
	public Location getBlockLookingAt(boolean inside);
	
	public boolean handleInteraction(Input input);
}
