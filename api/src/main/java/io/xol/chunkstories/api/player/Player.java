package io.xol.chunkstories.api.player;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.components.Subscriber;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.item.inventory.Inventory;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.plugin.commands.CommandEmitter;
import io.xol.chunkstories.api.server.ServerInterface;
import io.xol.chunkstories.api.world.World;

//(c) 2015-2017 XolioWare Interactive
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
	public EntityControllable getControlledEntity();
	
	/**
	 * Sets the entity this player has control over (and tells him)
	 * @return 
	 */
	public boolean setControlledEntity(EntityControllable entity);
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
	
	public boolean isConnected();

	public boolean hasSpawned();

	public void updateTrackedEntities();

	public ServerInterface getServer();
	
	public World getWorld();

	public void openInventory(Inventory inventory);
}
