package io.xol.chunkstories.content;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.events.Event;
import io.xol.chunkstories.api.events.EventExecutor;
import io.xol.chunkstories.api.events.EventHandler;
import io.xol.chunkstories.api.events.EventListeners;
import io.xol.chunkstories.api.events.Listener;
import io.xol.chunkstories.api.events.RegisteredListener;
import io.xol.chunkstories.api.plugin.ChunkStoriesPlugin;
import io.xol.chunkstories.api.plugin.PluginInformation;
import io.xol.chunkstories.api.plugin.PluginManager;
import io.xol.chunkstories.api.plugin.PluginStore;
import io.xol.chunkstories.api.plugin.commands.Command;
import io.xol.chunkstories.api.plugin.commands.CommandEmitter;
import io.xol.chunkstories.api.plugin.commands.CommandHandler;
import io.xol.chunkstories.api.server.ServerInterface;
import io.xol.chunkstories.tools.ChunkStoriesLogger;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PluginsManager implements PluginManager
{
	ClientInterface client;
	ServerInterface server;
	PluginStore store = new PluginStore();

	public Set<ChunkStoriesPlugin> activePlugins = new HashSet<ChunkStoriesPlugin>();
	
	public Map<String, Command> commandsAliases = new HashMap<String, Command>();
	public Map<Command, CommandHandler> commandsHandlers = new HashMap<Command, CommandHandler>();

	public PluginsManager(ClientInterface client)
	{
		this.client = client;
		reloadClientPlugins();
	}
	
	public PluginsManager(ServerInterface server)
	{
		this.server = server;
		File pluginsFolder = new File(GameDirectory.getGameFolderPath() + "/plugins/");
		pluginsFolder.mkdirs();
		
		// reloadServerPlugins();
		//store.loadPlugins(pluginsFolder);
	}

	public void enablePlugins()
	{
		Set<PluginInformation> pluginsToInitialize = store.getLoadedPlugins();
		System.out.println(pluginsToInitialize.size() + " plugins to initialize");
		for (PluginInformation pj : pluginsToInitialize)
		{
			ChunkStoriesPlugin plugin = pj.createInstance(this);
			plugin.setServer(server);
			// Add commands support
			for (Command command : pj.commands)
			{
				for(String alias : command.aliases())
				{
					System.out.println("alias"+alias+" 4cmd"+command);
					commandsAliases.put(alias, command);
					
				}
				
				if (commandsHandlers.containsKey(command))
				{
					System.out.println("Plugin " + pj.getName() + " can't define the command " + command.getName() + " as it's already defined by another plugin.");
					continue;
				}
				else
					commandsHandlers.put(command, plugin);
			}

			activePlugins.add(plugin);
			// Ignore dependancies for now, just load those suckers as they come
			// TODO don't
			plugin.onEnable();
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
		commandsHandlers.clear();
	}

	@Override
	public void reloadPlugins()
	{
		if(client != null)
			reloadClientPlugins();
		if(server != null)
			reloadServerPlugins();
	}
	
	public void reloadClientPlugins()
	{
		disablePlugins();
		store.unloadPlugins();
		//TODO load plugins from mods/ folder
		store.loadPlugins(new File(GameDirectory.getGameFolderPath() + "/res/client/"));
		enablePlugins();
	}

	public void reloadServerPlugins()
	{
		disablePlugins();
		store.unloadPlugins();
		store.loadPlugins(new File(GameDirectory.getGameFolderPath() + "/plugins/"));
		enablePlugins();
	}

	public void registerCommandHandler(String commandName, CommandHandler commandHandler)
	{
		Command command = findCommandUsingAlias(commandName);
		if(command == null)
			return;
	
		commandsHandlers.put(command, commandHandler);
	}

	@Override
	public boolean dispatchCommand(CommandEmitter emitter, String commandName, String[] arguments)
	{
		/*String cmdName = unparsedCommandText.toLowerCase();
		String[] args = {};
		if (unparsedCommandText.contains(" "))
		{
			cmdName = unparsedCommandText.substring(0, unparsedCommandText.indexOf(" "));
			args = unparsedCommandText.substring(unparsedCommandText.indexOf(" ")+1, unparsedCommandText.length()).split(" ");
		}*/
		
		Command command = findCommandUsingAlias(commandName);
		System.out.println(command);
		if(command == null)
			return false;
		
		//System.out.println("debug looking for plugin to handle cmd" + cmdName + " args:" + args.length);
		
		//Command command = new Command(cmdName);
		if (commandsHandlers.containsKey(command))
		{
			try
			{
				return commandsHandlers.get(command).handleCommand(emitter, command, arguments);
			}
			catch (Throwable t)
			{
				emitter.sendMessage("#FF4040 An exception was throwed when handling your command "+t.getMessage());
				t.printStackTrace();
			}
		}
		return false;
	}

	private Command findCommandUsingAlias(String commandName)
	{
		return commandsAliases.get(commandName.toLowerCase());
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
				//System.out.println("Checking out "+method);
				
				EventHandler eh = method.getAnnotation(EventHandler.class);
				if(eh == null)
					continue;
				//System.out.println("has correct annotation");
				
				//TODO something about priority
				if(method.getParameterTypes().length != 1 || !Event.class.isAssignableFrom(method.getParameterTypes()[0]))
				{
					ChunkStoriesLogger.getInstance().warning("Plugin "+plugin+" attempted to register an invalid EventHandler");
					continue;
				}
				Class<? extends Event> parameter = method.getParameterTypes()[0].asSubclass(Event.class);
				// Create an EventExecutor to launch the event code
				EventExecutor executor = new EventExecutor(){
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
				ChunkStoriesLogger.getInstance().info("Successuflly added EventHandler in "+listener+" of plugin "+plugin);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void fireEvent(Event event)
	{
		EventListeners listeners = event.getListeners();
		
		for(RegisteredListener listener : listeners.getListeners())
		{
			listener.invokeForEvent(event);
		}
		
		//If we didn't surpress it's behaviour
		//if(event.isAllowedToExecute())
		
		event.defaultBehaviour();
	}
}
