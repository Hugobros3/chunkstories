package io.xol.chunkstories.api.plugin;

import io.xol.chunkstories.api.events.Event;
import io.xol.chunkstories.api.events.Listener;
import io.xol.chunkstories.api.plugin.commands.CommandEmitter;
import io.xol.chunkstories.api.plugin.commands.CommandHandler;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface PluginManager
{
	public void disablePlugins();
	public void reloadPlugins();

	/**
	 * Dispatches an command to the plugins
	 * @param cmd The command line
	 * @param emitter Whoever sent it
	 * @return
	 */
	public boolean dispatchCommand(CommandEmitter emitter, String commandName, String[] arguments);
	
	public void registerCommandHandler(String commandName, CommandHandler commandHandler);
	
	/**
	 * Register a Listener in an plugin
	 * @param l
	 * @param plugin
	 */
	public void registerEventListener(Listener l, ChunkStoriesPlugin plugin);
	
	/**
	 * Fires an Event, pass it to all plugins that are listening for this kind of event
	 * @param event
	 */
	public void fireEvent(Event event);
}
