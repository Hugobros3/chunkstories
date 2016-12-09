package io.xol.chunkstories.content;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;

import io.xol.chunkstories.api.events.Event;
import io.xol.chunkstories.api.events.EventExecutor;
import io.xol.chunkstories.api.events.EventHandler;
import io.xol.chunkstories.api.events.EventListeners;
import io.xol.chunkstories.api.events.Listener;
import io.xol.chunkstories.api.events.RegisteredListener;
import io.xol.chunkstories.api.exceptions.plugins.PluginCreationException;
import io.xol.chunkstories.api.exceptions.plugins.PluginLoadException;
import io.xol.chunkstories.api.plugin.ChunkStoriesPlugin;
import io.xol.chunkstories.api.plugin.ClientPluginManager;
import io.xol.chunkstories.api.plugin.PluginInformation;
import io.xol.chunkstories.api.plugin.PluginInformation.PluginType;
import io.xol.chunkstories.api.plugin.PluginManager;
import io.xol.chunkstories.api.plugin.ServerPluginManager;
import io.xol.chunkstories.api.plugin.commands.Command;
import io.xol.chunkstories.api.plugin.commands.CommandEmitter;
import io.xol.chunkstories.api.plugin.commands.CommandHandler;
import io.xol.chunkstories.api.plugin.context.PluginExecutionContext;
import io.xol.chunkstories.tools.ChunkStoriesLogger;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class DefaultPluginManager implements PluginManager
{
	//PluginStore store = new PluginStore();
	private final PluginExecutionContext pluginExecutionContext;

	public Set<ChunkStoriesPlugin> activePlugins = new HashSet<ChunkStoriesPlugin>();

	public Map<String, Command> commandsAliases = new HashMap<String, Command>();
	public Set<Command> commands = new HashSet<Command>();

	public DefaultPluginManager(PluginExecutionContext pluginExecutionContext)
	{
		this.pluginExecutionContext = pluginExecutionContext;
	}

	@Override
	public Collection<ChunkStoriesPlugin> loadedPlugins()
	{
		return activePlugins;
	}

	//public Map<Command, CommandHandler<?>> commandsHandlers = new HashMap<Command, CommandHandler<?>>();

	/*public DefaultPluginManager(ClientInterface client)
	{
		this.client = client;
		reloadClientPlugins();
	}
	
	public DefaultPluginManager(ServerInterface server)
	{
		this.server = server;
		File pluginsFolder = new File(GameDirectory.getGameFolderPath() + "/plugins/");
		pluginsFolder.mkdirs();
	
		// reloadServerPlugins();
		//store.loadPlugins(pluginsFolder);
	}*/

	@Override
	/** Implementation : Checks %gamedir%/plugins/ folder, loads what it can within, checks enabled mods and their plugins/ folder, enables what it can */
	public void reloadPlugins()
	{
		//First disable any leftover plugins
		disablePlugins();

		//We make a list
		LinkedList<PluginInformation> pluginsToLoad = new LinkedList<PluginInformation>();

		//Creates plugins folder if it isn't present.
		File pluginsFolder = new File(GameDirectory.getGameFolderPath() + "/plugins/");
		pluginsFolder.mkdirs();

		//Iterates over files of the folder
		for (File file : pluginsFolder.listFiles())
		{
			if (file.getName().endsWith(".jar"))
			{
				try
				{
					PluginInformation pluginInformation = new PluginInformation(file, this.getClass().getClassLoader());
					//Checks type is appropriate

					//Client only plugins require actually being a client
					if (pluginInformation.getPluginType() == PluginType.CLIENT_ONLY && !(this instanceof ClientPluginManager))
						continue;
					//Server only plugins require the same
					else if (pluginInformation.getPluginType() == PluginType.SERVER_ONLY && !(this instanceof ServerPluginManager))
						continue;

					pluginsToLoad.add(pluginInformation);
				}
				catch (PluginLoadException | IOException e)
				{
					ChunkStoriesLogger.getInstance().error("Failed to load plugin file " + file + e.getMessage());
					e.printStackTrace();
				}
			}
		}

		//Mods too can bundle plugins
		for (PluginInformation pluginInformation : ModsManager.getModsPlugins())
		{
			//Client only plugins require actually being a client
			if (pluginInformation.getPluginType() == PluginType.CLIENT_ONLY && !(this instanceof ClientPluginManager))
				continue;
			//Server only plugins require the same
			else if (pluginInformation.getPluginType() == PluginType.SERVER_ONLY && !(this instanceof ServerPluginManager))
				continue;

			pluginsToLoad.add(pluginInformation);
		}

		//Enables the found plugins
		enablePlugins(pluginsToLoad);
	}

	private void enablePlugins(LinkedList<PluginInformation> pluginsToInitialize)
	{
		ChunkStoriesLogger.getInstance().info(pluginsToInitialize.size() + " plugins to initialize");

		Deque<PluginInformation> order = new LinkedBlockingDeque<PluginInformation>();

		//TODO sort plugins requirements (requires/before)
		for (PluginInformation pluginInformation : pluginsToInitialize)
		{
			order.add(pluginInformation);
		}

		//Loads each provided plugin
		for (PluginInformation pluginInformation : order)
		{
			try
			{
				// Instanciate the plugin after all
				ChunkStoriesPlugin pluginInstance = pluginInformation.createInstance(pluginExecutionContext);

				// Add commands support
				for (Command command : pluginInformation.commands)
				{
					//Checks the command isn't already defined
					if (commands.contains(command))
					{
						ChunkStoriesLogger.getInstance().warning("Plugin " + pluginInformation.getName() + " can't define the command " + command.getName() + " as it's already defined by another plugin.");
						continue;
					}

					//The default handler
					command.setHandler(pluginInstance);

					commands.add(command);

					for (String alias : command.aliases())
						if (commandsAliases.put(alias, command) != null)
							ChunkStoriesLogger.getInstance().warning("Plugin " + pluginInformation + " tried to register alias " + alias + " for command " + command + ".");

				}

				activePlugins.add(pluginInstance);
				pluginInstance.onEnable();
			}
			catch (PluginCreationException pce)
			{
				ChunkStoriesLogger.getInstance().error("Couldn't create plugin " + pluginInformation + " : " + pce.getMessage());
				pce.printStackTrace();
			}
		}

	}

	@Override
	public void disablePlugins()
	{
		for (ChunkStoriesPlugin plugin : activePlugins)
		{
			plugin.onDisable();
		}
		activePlugins.clear();
		commandsAliases.clear();
		commands.clear();
	}

	public void registerCommandHandler(String commandName, CommandHandler commandHandler)
	{
		Command command = findCommandUsingAlias(commandName);
		if (command == null)
		{
			ChunkStoriesLogger.getInstance().warning("Can't register CommandHandler " + commandHandler + " for command " + commandName + " : Command isn't defined.");
			return;
		}

		command.setHandler(commandHandler);
	}

	@Override
	public boolean dispatchCommand(CommandEmitter emitter, String commandName, String[] arguments)
	{
		Command command = findCommandUsingAlias(commandName);
		if (command == null)
			return false;

		try
		{
			return command.getHandler().handleCommand(emitter, command, arguments);
		}
		catch (Throwable t)
		{
			emitter.sendMessage("#FF4040 An exception was throwed when handling your command " + t.getMessage());
			t.printStackTrace();
		}

		return false;
	}

	public Command findCommandUsingAlias(String commandName)
	{
		return commandsAliases.get(commandName.toLowerCase());
	}

	@Override
	public void registerEventListener(Listener listener, ChunkStoriesPlugin plugin)
	{
		System.out.println("Registering " + listener);
		try
		{
			// Get a list of all the classes methods
			Set<Method> methods = new HashSet<Method>();
			for (Method method : listener.getClass().getMethods())
				methods.add(method);
			for (Method method : listener.getClass().getDeclaredMethods())
				methods.add(method);
			// Filter it so only interested in @EventHandler annoted methods
			for (final Method method : methods)
			{
				//System.out.println("Checking out "+method);

				EventHandler eh = method.getAnnotation(EventHandler.class);
				if (eh == null)
					continue;
				//System.out.println("has correct annotation");

				//TODO something about priority
				if (method.getParameterTypes().length != 1 || !Event.class.isAssignableFrom(method.getParameterTypes()[0]))
				{
					ChunkStoriesLogger.getInstance().warning("Plugin " + plugin + " attempted to register an invalid EventHandler");
					continue;
				}
				Class<? extends Event> parameter = method.getParameterTypes()[0].asSubclass(Event.class);
				// Create an EventExecutor to launch the event code
				EventExecutor executor = new EventExecutor()
				{
					@Override
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
				ChunkStoriesLogger.getInstance().info("Successuflly added EventHandler in " + listener + " of plugin " + plugin);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void fireEvent(Event event)
	{
		EventListeners listeners = event.getListeners();

		for (RegisteredListener listener : listeners.getListeners())
		{
			listener.invokeForEvent(event);
		}

		//If we didn't surpress it's behaviour
		//if(event.isAllowedToExecute())

		event.defaultBehaviour();
	}

	@Override
	public Collection<Command> commands()
	{
		return commands;
	}
}
