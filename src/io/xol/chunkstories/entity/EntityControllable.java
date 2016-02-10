package io.xol.chunkstories.entity;

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
	
	public void controls(boolean focus);
}
