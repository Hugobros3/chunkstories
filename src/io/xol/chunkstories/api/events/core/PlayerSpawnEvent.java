package io.xol.chunkstories.api.events.core;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.events.Event;
import io.xol.chunkstories.api.events.EventListeners;
import io.xol.chunkstories.api.plugin.server.Player;
import io.xol.chunkstories.entity.Entity;
import io.xol.chunkstories.entity.EntityControllable;
import io.xol.chunkstories.entity.core.EntityPlayer;
import io.xol.chunkstories.server.Server;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PlayerSpawnEvent<CE extends Entity & EntityControllable> extends Event
{
	// Every event class has to have this
	
	static EventListeners listeners = new EventListeners();
	
	public EventListeners getListeners()
	{
		return listeners;
	}
	
	public static EventListeners getListenersStatic()
	{
		return listeners;
	}
	
	// Specific event code
	
	@SuppressWarnings("rawtypes")
	public Player player;
	public Location spawnLocation;
	public Entity entity;
	
	public PlayerSpawnEvent(Player player, Location location)
	{
		this.player = player;
		this.spawnLocation = player.getPosition();
		this.entity = new EntityPlayer(Server.getInstance().world, 0d, 0d, 0d, player.getName());
	}

	public Player getPlayer()
	{
		return player;
	}
	
	@Override
	public void defaultBehaviour()
	{
		entity.setLocation(spawnLocation);
		player.setControlledEntity((CE) entity);
		System.out.println("Created entity named "+entity+":"+player.getDisplayName());
	}
	
}
