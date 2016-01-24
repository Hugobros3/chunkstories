package io.xol.chunkstories.api.events;

import io.xol.chunkstories.api.plugin.ChunkStoriesPlugin;
import io.xol.chunkstories.tools.ChunkStoriesLogger;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class RegisteredListener
{
	Listener listener;
	ChunkStoriesPlugin plugin;
	EventExecutor executor;

	public RegisteredListener(Listener l, ChunkStoriesPlugin plugin, EventExecutor executor)
	{
		this.listener = l;
		this.plugin = plugin;
		this.executor = executor;
	}

	public void invokeForEvent(Event event)
	{
		try
		{
			executor.fireEvent(event);
		}
		catch (Exception e)
		{
			ChunkStoriesLogger.getInstance().warning("Je met les warnings de la blague : ");
			e.printStackTrace();
		}
	}
}
