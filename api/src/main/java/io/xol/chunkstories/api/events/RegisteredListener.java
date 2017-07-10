package io.xol.chunkstories.api.events;

import io.xol.chunkstories.api.plugin.ChunkStoriesPlugin;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** Describes a successfully processed event handler annotation, called when a specific event happens */
public class RegisteredListener
{
	Listener listener;
	ChunkStoriesPlugin plugin;
	EventExecutor executor;
	EventHandler.EventPriority priority;

	public RegisteredListener(Listener listener, ChunkStoriesPlugin plugin, EventExecutor executor, EventHandler.EventPriority priority)
	{
		this.listener = listener;
		this.plugin = plugin;
		this.executor = executor;
		this.priority = priority;
	}

	public void invokeForEvent(Event event) throws Exception
	{
		executor.fireEvent(event);
	}
}
