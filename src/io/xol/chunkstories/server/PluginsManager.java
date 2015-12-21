package io.xol.chunkstories.server;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.xol.chunkstories.GameDirectory;
import io.xol.chunkstories.api.Command;
import io.xol.chunkstories.api.JavaPlugin;
import io.xol.chunkstories.api.PluginJar;
import io.xol.chunkstories.api.PluginManager;
import io.xol.chunkstories.api.PluginStore;
import io.xol.chunkstories.api.events.Listener;
import io.xol.chunkstories.server.tech.CommandEmitter;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PluginsManager implements PluginManager
{
	Server server;
	PluginStore store = new PluginStore();

	Set<JavaPlugin> activePlugins = new HashSet<JavaPlugin>();
	Map<Command, JavaPlugin> commandsHandlers = new HashMap<Command, JavaPlugin>();

	public PluginsManager(Server server)
	{
		this.server = server;
		File pluginsFolder = new File(GameDirectory.getGameFolderPath() + "/plugins/");
		pluginsFolder.mkdirs();
		store.loadPlugins(pluginsFolder, false);

		enablePlugins();
	}

	private void enablePlugins()
	{
		Set<PluginJar> pluginsToInitialize = store.getLoadedPlugins();
		System.out.println(pluginsToInitialize.size() + " plugins to initialize");
		for (PluginJar pj : pluginsToInitialize)
		{
			JavaPlugin plugin = pj.getInstance();
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
		for (JavaPlugin plugin : activePlugins)
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
				commandsHandlers.get(command).handleCommand(command, args, cmd);
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
	public void registerEventListener(Listener l, JavaPlugin plugin)
	{
		
	}
}
