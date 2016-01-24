package io.xol.chunkstories.server;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.xol.chunkstories.GameDirectory;
import io.xol.chunkstories.api.events.Event;
import io.xol.chunkstories.api.events.EventExecutor;
import io.xol.chunkstories.api.events.EventHandler;
import io.xol.chunkstories.api.events.EventListeners;
import io.xol.chunkstories.api.events.Listener;
import io.xol.chunkstories.api.events.RegisteredListener;
import io.xol.chunkstories.api.plugin.ChunkStoriesPlugin;
import io.xol.chunkstories.api.plugin.PluginJar;
import io.xol.chunkstories.api.plugin.PluginManager;
import io.xol.chunkstories.api.plugin.PluginStore;
import io.xol.chunkstories.api.plugin.server.Command;
import io.xol.chunkstories.server.tech.CommandEmitter;
import io.xol.chunkstories.tools.ChunkStoriesLogger;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PluginsManager implements PluginManager
{
	Server server;
	PluginStore store = new PluginStore();

	Set<ChunkStoriesPlugin> activePlugins = new HashSet<ChunkStoriesPlugin>();
	Map<Command, ChunkStoriesPlugin> commandsHandlers = new HashMap<Command, ChunkStoriesPlugin>();

	public PluginsManager(Server server)
	{
		this.server = server;
		File pluginsFolder = new File(GameDirectory.getGameFolderPath() + "/plugins/");
		pluginsFolder.mkdirs();
		store.loadPlugins(pluginsFolder, false);
	}

	public void enablePlugins()
	{
		Set<PluginJar> pluginsToInitialize = store.getLoadedPlugins();
		System.out.println(pluginsToInitialize.size() + " plugins to initialize");
		for (PluginJar pj : pluginsToInitialize)
		{
			ChunkStoriesPlugin plugin = pj.getInstance();
			plugin.setPluginManager(this);
			plugin.setServer(server);
			// Add commands support
			for (Command cmd : pj.commands)
			{
				if (commandsHandlers.containsKey(cmd))
				{
					System.out.println("Plugin " + pj.getName() + " can't define the command " + cmd.getName() + " as it's already defined by another plugin.");
					break;
				}
				else
					commandsHandlers.put(cmd, plugin);
			}

			activePlugins.add(plugin);
			// Ignore dependancies for now, just load those suckers as they come
			// TODO don't
			plugin.onEnable();
		}
	}

	public void disablePlugins()
	{
		for (ChunkStoriesPlugin plugin : activePlugins)
		{
			plugin.onDisable();
		}
		activePlugins.clear();
		commandsHandlers.clear();
	}

	public void reloadPlugins()
	{
		disablePlugins();
		store.loadPlugins(new File(GameDirectory.getGameFolderPath() + "/plugins/"), true);
		enablePlugins();
	}

	public boolean dispatchCommand(String cmd, CommandEmitter emitter)
	{
		String cmdName = cmd.toLowerCase();
		String[] args = {};
		if (cmd.contains(" "))
		{
			cmdName = cmd.substring(0, cmd.indexOf(" "));
			args = cmd.substring(cmd.indexOf(" "), cmd.length()).split(" ");
		}
		//System.out.println("debug looking for plugin to handle cmd" + cmdName + " args:" + args.length);
		Command command = new Command(cmdName);
		if (commandsHandlers.containsKey(command))
		{
			try
			{
				commandsHandlers.get(command).handleCommand(emitter, command, args, cmd);
			}
			catch (Exception e)
			{
				emitter.sendMessage("#FF4040 An exception was throwed when handling your command "+e.getMessage());
			}
			return true;
		}
		return false;
	}

	@Override
	public void registerEventListener(Listener listener, ChunkStoriesPlugin plugin)
	{
		System.out.println("Registering "+listener);
		try{
			// Get a list of all the classes methods
			Set<Method> methods = new HashSet<Method>();
			for(Method method : listener.getClass().getMethods())
				methods.add(method);
			for(Method method : listener.getClass().getDeclaredMethods())
				methods.add(method);
			// Filter it so only interested in @EventHandler annoted methods
			for(final Method method : methods)
			{
				System.out.println("Checking out "+method);
				EventHandler eh = method.getAnnotation(EventHandler.class);
				if(eh == null)
					continue;
				System.out.println("has correct annotation");
				//TODO something about priority
				if(method.getParameterTypes().length != 1 || !Event.class.isAssignableFrom(method.getParameterTypes()[0]))
				{
					ChunkStoriesLogger.getInstance().warning("Plugin "+plugin+" attempted to register an invalid EventHandler");
					continue;
				}
				Class<? extends Event> parameter = method.getParameterTypes()[0].asSubclass(Event.class);
				// Create an EventExecutor to launch the event code
				EventExecutor executor = new EventExecutor(){
					public void fireEvent(Event event) throws Exception
					{
						method.invoke(listener, event);
					}
				};
				RegisteredListener re = new RegisteredListener(listener, plugin, executor);
				// Get the listeners list for this event
				Method getListeners = parameter.getMethod("getListenersStatic");
				getListeners.setAccessible(true);
				EventListeners thisEventKindOfListeners = (EventListeners) getListeners.invoke(null);
				// Add our own to it
				thisEventKindOfListeners.registerListener(re);
				ChunkStoriesLogger.getInstance().warning("Successuflly added EventHandler in "+listener+" of plugin "+plugin);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public boolean fireEvent(Event event)
	{
		EventListeners listeners = event.getListeners();
		
		for(RegisteredListener listener : listeners.getListeners())
		{
			listener.invokeForEvent(event);
		}
		
		return event.isAllowedToExecute();
	}
}
