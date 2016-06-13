package io.xol.chunkstories.api.server;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.components.Subscriber;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.server.tech.CommandEmitter;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * This interface is a player connected to a server, viewed from that server.
 */
public interface Player extends CommandEmitter, Controller, Subscriber, PacketDestinator, PacketSender
{
	/**
	 * Returns the username of the player
	 * @return
	 */
	@Override
	public String getName();

	/**
	 * Returns the displayable name of the player (including things like tags, color etc)
	 * @return
	 */
	public String getDisplayName();
	
	/**
	 * Returns the entity this player is controlling
	 * @return
	 */
	public Entity getControlledEntity();
	
	/**
	 * Sets the entity this player has control over (and tells him)
	 */
	public void setControlledEntity(Entity entity);
	/**
	 * Sends a text message to this player chat
	 * @param msg
	 */
	@Override
	public void sendMessage(String msg);
	
	/**
	 * Gets the location of the user
	 * @return a {@link Location} object
	 */
	public Location getLocation();
	
	/**
	 * Sets the location of the user
	 * @param l a {@link Location} object
	 */
	public void setLocation(Location l);
	
	public void setFlying(boolean flying);
	
	/**
	 * Kicks the player
	 * @param reason
	 */
	public void kickPlayer(String reason);
	
	public boolean isConnected();
}
