package io.xol.chunkstories.api.events.rendering;

import io.xol.chunkstories.api.events.Event;
import io.xol.chunkstories.api.events.EventListeners;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.WorldRenderer;
import io.xol.chunkstories.api.world.World;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class WorldPostRenderingEvent extends Event
{
	// Every event class has to have this
	
	static EventListeners listeners = new EventListeners(WorldPostRenderingEvent.class);
	
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
	private WorldRenderer worldRenderer;
	private RenderingInterface renderingInterface;
	
	public WorldPostRenderingEvent(World world, WorldRenderer worldRenderer, RenderingInterface renderingInterface)
	{
		super();
		this.world = world;
		this.worldRenderer = worldRenderer;
		this.renderingInterface = renderingInterface;
	}
	
	public World getWorld()
	{
		return world;
	}

	public WorldRenderer getWorldRenderer()
	{
		return worldRenderer;
	}

	public RenderingInterface getRenderingInterface()
	{
		return renderingInterface;
	}
}
