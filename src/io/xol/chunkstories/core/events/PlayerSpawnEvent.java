package io.xol.chunkstories.core.events;

import io.xol.chunkstories.api.events.CancellableEvent;
import io.xol.chunkstories.api.events.EventListeners;
import io.xol.chunkstories.api.server.Player;
import io.xol.chunkstories.world.WorldImplementation;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PlayerSpawnEvent extends CancellableEvent
{
	// Every event class has to have this
	static EventListeners listeners = new EventListeners();
	
	@Override
	public EventListeners getListeners()
	{
		return listeners;
	}
	
	public static EventListeners getListenersStatic()
	{
		return listeners;
	}
	
	// Specific event code
	
	public Player player;
	public WorldImplementation world;
	
	public PlayerSpawnEvent(Player player, WorldImplementation world)
	{
		this.player = player;
		this.world = world;
		//this.entity = new EntityPlayer(Server.getInstance().world, 0d, 0d, 0d, player.getName());
	}

	public Player getPlayer()
	{
		return player;
	}
	
}
