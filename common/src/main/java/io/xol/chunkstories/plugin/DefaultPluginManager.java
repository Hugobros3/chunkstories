//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.plugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.GameContext;
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
import io.xol.chunkstories.api.plugin.PluginInformation.PluginType;
import io.xol.chunkstories.api.plugin.PluginManager;
import io.xol.chunkstories.api.plugin.ServerPluginManager;
import io.xol.chunkstories.api.plugin.commands.Command;
import io.xol.chunkstories.api.plugin.commands.CommandEmitter;
import io.xol.chunkstories.api.plugin.commands.CommandHandler;
import io.xol.chunkstories.api.plugin.commands.SystemCommand;
import io.xol.chunkstories.api.util.IterableIterator;
import io.xol.chunkstories.content.GameDirectory;
import io.xol.chunkstories.content.mods.ModsManagerImplementation;

public abstract class DefaultPluginManager implements PluginManager {
	// PluginStore store = new PluginStore();
	private final GameContext pluginExecutionContext;

	public Set<ChunkStoriesPlugin> activePlugins = new HashSet<ChunkStoriesPlugin>();
	private HashMap<EventListeners, RegisteredListener> registeredEventListeners = new HashMap<EventListeners, RegisteredListener>();

	public Map<String, Command> commandsAliases = new HashMap<String, Command>();
	public Set<Command> commands = new HashSet<Command>();

	private final static Logger pluginsLogger = LoggerFactory.getLogger("plugins");

	public DefaultPluginManager(GameContext pluginExecutionContext) {
		this.pluginExecutionContext = pluginExecutionContext;
	}

	@Override
	public IterableIterator<ChunkStoriesPlugin> activePlugins() {
		return new IterableIterator<ChunkStoriesPlugin>() {

			Iterator<ChunkStoriesPlugin> i = activePlugins.iterator();

			@Override
			public boolean hasNext() {
				return i.hasNext();
			}

			@Override
			public ChunkStoriesPlugin next() {
				return i.next();
			}

		};
	}

	@Override
	/**
	 * Implementation : Checks %gamedir%/plugins/ folder, loads what it can within,
	 * checks enabled mods and their plugins/ folder, enables what it can
	 */
	public void reloadPlugins() {
		// First disable any leftover plugins
		disablePlugins();

		// We make a list
		LinkedList<PluginInformationImplementation> pluginsToLoad = new LinkedList<PluginInformationImplementation>();

		// Creates plugins folder if it isn't present.
		File pluginsFolder = new File(GameDirectory.getGameFolderPath() + "/plugins/");
		pluginsFolder.mkdirs();

		// Iterates over files of the folder
		for (File file : pluginsFolder.listFiles()) {
			if (file.getName().endsWith(".jar")) {
				try {
					PluginInformationImplementation pluginInformation = new PluginInformationImplementation(file,
							((ModsManagerImplementation) pluginExecutionContext.getContent().modsManager())
									.getFinalClassLoader());
					// Checks type is appropriate

					// Client only plugins require actually being a client
					if (pluginInformation.getPluginType() == PluginType.CLIENT_ONLY
							&& !(this instanceof ClientPluginManager))
						continue;
					// Server only plugins require the same
					else if (pluginInformation.getPluginType() == PluginType.MASTER
							&& !(this instanceof ServerPluginManager))
						continue;

					pluginsToLoad.add(pluginInformation);
				} catch (PluginLoadException | IOException e) {
					logger().error("Failed to load plugin file " + file + e.getMessage());
					e.printStackTrace();
				}
			}
		}

		// Mods too can bundle plugins
		for (PluginInformationImplementation pluginInformation : ((ModsManagerImplementation) this.pluginExecutionContext
				.getContent().modsManager()).getAllModsPlugins()) {
			// Client only plugins require actually being a client
			if (pluginInformation.getPluginType() == PluginType.CLIENT_ONLY && !(this instanceof ClientPluginManager))
				continue;
			// Server only plugins require the same
			else if (pluginInformation.getPluginType() == PluginType.MASTER && !(this instanceof ServerPluginManager))
				continue;

			pluginsToLoad.add(pluginInformation);
		}

		// Enables the found plugins
		enablePlugins(pluginsToLoad);
	}

	private void enablePlugins(LinkedList<PluginInformationImplementation> pluginsToInitialize) {
		logger().info(pluginsToInitialize.size() + " plugins to initialize");

		Deque<PluginInformationImplementation> order = new LinkedBlockingDeque<PluginInformationImplementation>();

		// TODO sort plugins requirements (requires/before)
		for (PluginInformationImplementation pluginInformation : pluginsToInitialize) {
			order.add(pluginInformation);
		}

		// Loads each provided plugin
		for (PluginInformationImplementation pluginInformation : order) {
			try {
				// Add commands support
				for (Command command : pluginInformation.getCommands()) {
					// Checks the command isn't already defined
					if (commands.contains(command)) {
						logger().warn("Plugin " + pluginInformation.getName() + " can't define the command "
								+ command.getName() + " as it's already defined by another plugin.");
						continue;
					}

					commands.add(command);

					for (String alias : command.aliases())
						if (commandsAliases.put(alias, command) != null)
							logger().warn("Plugin " + pluginInformation + " tried to register alias " + alias
									+ " for command " + command + ".");

				}

				// Instanciate the plugin after getAllVoxelComponents
				ChunkStoriesPlugin pluginInstance = pluginInformation.createInstance(pluginExecutionContext);

				activePlugins.add(pluginInstance);
				pluginInstance.onEnable();
			} catch (PluginCreationException pce) {
				logger().error("Couldn't create plugin " + pluginInformation + " : " + pce.getMessage());
				pce.printStackTrace();
			}
		}

	}

	@Override
	public void disablePlugins() {
		// Call onDisable for plugins
		for (ChunkStoriesPlugin plugin : activePlugins)
			plugin.onDisable();

		// Remove one by one each listener
		for (Entry<EventListeners, RegisteredListener> e : registeredEventListeners.entrySet()) {
			e.getKey().unRegisterListener(e.getValue());
		}

		// Remove registered commands
		// TODO only remove plugins commands
		commandsAliases.clear();
		commands.clear();

		// At last clear the plugins list
		activePlugins.clear();
	}

	public void registerCommandHandler(String commandName, CommandHandler commandHandler) {
		Command command = findCommandUsingAlias(commandName);
		if (command == null) {
			logger().warn("Can't register CommandHandler " + commandHandler + " for command " + commandName
					+ " : Command isn't defined.");
			return;
		}

		command.setHandler(commandHandler);
	}

	@Override
	public SystemCommand registerCommand(String commandName, String... aliases) {
		SystemCommand command = new SystemCommand(pluginExecutionContext, commandName);
		for (String alias : aliases) {
			command.addAlias(alias);
			commandsAliases.put(alias, command);
		}

		this.commands.add(command);
		commandsAliases.put(commandName, command);

		return command;
	}

	// @Override
	public void unregisterCommand(Command command) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean dispatchCommand(CommandEmitter emitter, String commandName, String[] arguments) {
		Command command = findCommandUsingAlias(commandName);
		if (command == null)
			return false;

		try {
			CommandHandler handler = command.getHandler();

			if (handler != null)
				return handler.handleCommand(emitter, command, arguments);
			else
				emitter.sendMessage("#FF2020No handler defined for this command !");

			return false;
		} catch (Throwable t) {
			emitter.sendMessage("#FF4040 An exception was throwed when handling your command " + t.getMessage());
			t.printStackTrace();
		}

		return false;
	}

	public Command findCommandUsingAlias(String commandName) {
		return commandsAliases.get(commandName.toLowerCase());
	}

	@Override
	public void registerEventListener(Listener listener, ChunkStoriesPlugin plugin) {
		System.out.println("Registering " + listener);
		try {
			// Get a list of getAllVoxelComponents the classes methods
			Set<Method> methods = new HashSet<Method>();
			for (Method method : listener.getClass().getMethods())
				methods.add(method);
			for (Method method : listener.getClass().getDeclaredMethods())
				methods.add(method);
			// Filter it so only interested in @EventHandler annoted methods
			for (final Method method : methods) {
				EventHandler eventHandlerAnnotation = method.getAnnotation(EventHandler.class);

				// We look for the annotation in the method
				if (eventHandlerAnnotation == null)
					continue;

				// TODO something about priority
				if (method.getParameterTypes().length != 1
						|| !Event.class.isAssignableFrom(method.getParameterTypes()[0])) {
					logger().warn("Plugin " + plugin + " attempted to register an invalid EventHandler");
					continue;
				}
				Class<? extends Event> parameter = method.getParameterTypes()[0].asSubclass(Event.class);
				// Create an EventExecutor to launch the event code
				EventExecutor executor = new EventExecutor() {
					@Override
					public void fireEvent(Event event) throws Exception {
						method.invoke(listener, event);
					}
				};
				RegisteredListener registeredListener = new RegisteredListener(listener, plugin, executor,
						eventHandlerAnnotation.priority());

				// Get the listeners list for this event
				Method getListeners = parameter.getMethod("getListenersStatic");
				getListeners.setAccessible(true);
				EventListeners thisEventKindOfListeners = (EventListeners) getListeners.invoke(null);

				// Add our own to it
				thisEventKindOfListeners.registerListener(registeredListener);
				registeredEventListeners.put(thisEventKindOfListeners, registeredListener);

				// Depending on the event configuration we may or may not care about the
				// children events
				if (eventHandlerAnnotation.listenToChildEvents() != EventHandler.ListenToChildEvents.NO)
					addRegisteredListenerToEventChildren(thisEventKindOfListeners, registeredListener,
							eventHandlerAnnotation.listenToChildEvents() == EventHandler.ListenToChildEvents.RECURSIVE);

				logger().info("Successuflly added EventHandler for " + parameter.getName() + "in " + listener
						+ " of plugin " + plugin);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void addRegisteredListenerToEventChildren(EventListeners listeners, RegisteredListener registeredListener,
			boolean recursive) {
		for (EventListeners el : listeners.getChildrens()) {
			el.registerListener(registeredListener);
			registeredEventListeners.put(el, registeredListener);

			if (recursive)
				addRegisteredListenerToEventChildren(el, registeredListener, true);
		}
	}

	@Override
	public void fireEvent(Event event) {
		EventListeners listeners = event.getListeners();

		for (RegisteredListener listener : listeners.getListeners()) {
			try {
				listener.invokeForEvent(event);
			} catch (InvocationTargetException e) {
				logger().warn("Exception while invoking event, in event handling body : "
						+ e.getTargetException().getMessage());
				// e.printStackTrace();
				e.getTargetException().printStackTrace();
			} catch (Exception e) {
				logger().warn("Exception while invoking event : " + e.getMessage());
				e.printStackTrace();
			}
		}

		// If we didn't surpress it's behaviour
		// if(event.isAllowedToExecute())
		// event.defaultBehaviour();
	}

	public Logger logger() {
		return pluginsLogger;
	}

	@Override
	public Collection<Command> commands() {
		return commands;
	}
}
