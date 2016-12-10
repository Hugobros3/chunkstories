package io.xol.chunkstories.core.events;

import io.xol.chunkstories.api.events.Event;
import io.xol.chunkstories.api.events.EventListeners;
import io.xol.chunkstories.api.world.World;

public class WorldTickEvent extends Event
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
	
	private World world;
	
	public WorldTickEvent(World world)
	{
		this.world = world;
	}

	public World getWorld()
	{
		return world;
	}
	
	@Override
	public void defaultBehaviour()
	{
		//Do nothing
	}
	
}
