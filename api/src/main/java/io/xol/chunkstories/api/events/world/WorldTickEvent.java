package io.xol.chunkstories.api.events.world;

import io.xol.chunkstories.api.events.Event;
import io.xol.chunkstories.api.events.EventListeners;
import io.xol.chunkstories.api.world.World;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class WorldTickEvent extends Event
{
	// Every event class has to have this

	static EventListeners listeners = new EventListeners(WorldTickEvent.class);

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
}
