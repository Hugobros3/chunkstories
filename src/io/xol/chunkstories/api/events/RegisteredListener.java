package io.xol.chunkstories.api.events;

import java.lang.reflect.InvocationTargetException;

import io.xol.chunkstories.api.plugin.ChunkStoriesPlugin;
import io.xol.chunkstories.tools.ChunkStoriesLogger;

//(c) 2015-2017 XolioWare Interactive
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
		catch (InvocationTargetException e)
		{
			ChunkStoriesLogger.getInstance().warning("Exception while invoking event, in event handling body : "+e.getTargetException().getMessage());
			//e.printStackTrace();
			e.getTargetException().printStackTrace();
		}
		catch (Exception e)
		{
			ChunkStoriesLogger.getInstance().warning("Exception while invoking event : "+e.getMessage());
			e.printStackTrace();
		}
	}
}
