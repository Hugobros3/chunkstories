package io.xol.chunkstories.entity;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.ClientController;
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.input.Input;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

/**
 * Defines an entity as controllable by players, this is used to allow the Client to send controls to the entity and the
 * server to update the client about it's entity status.
 * @author hugo@xol.io
 *
 */
public interface EntityControllable
{
	public Controller getController();
	
	public void setController(Controller controller);
	
	public void moveCamera(ClientController controller);
	
	public void tick(ClientController controller);
	
	/**
	 * If this entity has the ability to select blocks, this method should return said block
	 * @param inside If set to true it will return the selected block, if set to false it will return the one adjacent to
	 * @return
	 */
	public Location getBlockLookingAt(boolean inside);
	
	public boolean handleInteraction(Input input);
}
