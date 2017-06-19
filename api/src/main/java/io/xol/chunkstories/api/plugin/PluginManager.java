package io.xol.chunkstories.api.plugin;

import java.util.Collection;

import io.xol.chunkstories.api.events.Event;
import io.xol.chunkstories.api.events.Listener;
import io.xol.chunkstories.api.plugin.commands.Command;
import io.xol.chunkstories.api.plugin.commands.CommandEmitter;
import io.xol.chunkstories.api.plugin.commands.CommandHandler;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface PluginManager
{
	/** (Re)Loads all necessary plugins */
	public void reloadPlugins();
	
	/** Disables all enable plugins */
	public void disablePlugins();
	
	/** Lists active plugins */
	public Collection<ChunkStoriesPlugin> loadedPlugins();

	//Commands management
	
	/**
	 * Dispatches an command to the plugins
	 * @param emitter Whoever sent it
	 * @param commandName The command name
	 * @param arguments The arguments, splitted by spaces
	 * @return
	 */
	public boolean dispatchCommand(CommandEmitter emitter, String commandName, String[] arguments);
	
	/** Assigns a new command handler to a command */
	public void registerCommandHandler(String commandName, CommandHandler commandHandler);
	
	/** Returns the command matching this name or alias */
	public Command findCommandUsingAlias(String commandName);
	
	/** Returns a collection with all registered commands */
	public Collection<Command> commands();
	
	//Event handling ensues
	
	/**
	 * Register a Listener in an plugin
	 * @param l
	 * @param plugin
	 */
	public void registerEventListener(Listener listener, ChunkStoriesPlugin plugin);
	
	/**
	 * Fires an Event, pass it to all plugins that are listening for this kind of event
	 * @param event
	 */
	public void fireEvent(Event event);
}
