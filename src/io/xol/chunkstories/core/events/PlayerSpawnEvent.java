package io.xol.chunkstories.core.events;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.events.Event;
import io.xol.chunkstories.api.events.EventListeners;
import io.xol.chunkstories.api.server.Player;
import io.xol.chunkstories.core.entity.EntityPlayer;
import io.xol.chunkstories.server.Server;
import io.xol.chunkstories.world.WorldImplementation;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PlayerSpawnEvent extends Event
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
	
	@Override
	public void defaultBehaviour()
	{
		Entity entity = null;
		
		SerializedEntityFile playerEntityFile = new SerializedEntityFile("./players/" + player.getName().toLowerCase() + ".csf");
		if(playerEntityFile.exists())
			entity = playerEntityFile.read(world);
		
		if(entity == null)
		{
			System.out.println("Created entity named "+entity+":"+player.getDisplayName());
			entity = new EntityPlayer(world, 0d, 0d, 0d, player.getName());
			entity.setLocation(world.getDefaultSpawnLocation());
		}
		
		Server.getInstance().getWorld().addEntity(entity);
		player.setControlledEntity(entity);
		System.out.println("Added player entity");
	}
	
}
