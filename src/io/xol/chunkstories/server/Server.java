package io.xol.chunkstories.server;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

import io.xol.engine.misc.ColorsTools;
import io.xol.engine.misc.ConfigFile;
import io.xol.chunkstories.GameData;
import io.xol.chunkstories.GameDirectory;
import io.xol.chunkstories.VersionInfo;
import io.xol.chunkstories.api.plugin.server.Player;
import io.xol.chunkstories.api.plugin.server.ServerInterface;
import io.xol.chunkstories.server.net.ServerClient;
import io.xol.chunkstories.server.net.ServerConnectionsHandler;
import io.xol.chunkstories.server.tech.CommandEmitter;
import io.xol.chunkstories.server.tech.ServerConsole;
import io.xol.chunkstories.server.tech.UsersPrivileges;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.chunkstories.world.ChunksData;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class Server implements Runnable, ServerInterface, CommandEmitter
{
	// The server class handles and make the link between all server components
	// It also takes care of the command line input as it's the main thread,
	// thought the processing of command lines is handled by ServerConsole.java
	static Server server;

	public static Server getInstance()
	{
		return server;
	}

	public static void main(String a[])
	{
		server = new Server();
		server.run();
	}

	// Basic server stuff init !
	public ConfigFile serverConfig = new ConfigFile("config/server.cfg");
	public Logger log = Logger.getLogger("server");
	public AtomicBoolean running = new AtomicBoolean(true);
	public long initS = System.currentTimeMillis() / 1000; // <- uptime !

	public WorldServer world;

	public ServerConnectionsHandler handler;
	public PluginsManager pluginsManager;

	// Sleeper thread to keep servers list updated
	public ServerAnnouncerThread announcer;

	public void run()
	{
		// logger init
		initLog();
		// Start server services
		try
		{
			log.info("Starting ChunkStories server ");
			handler = new ServerConnectionsHandler();
			// TODO make this a configurable thing
			ChunksData.CACHE_SIZE = 2048;
			Calendar cal = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("YYYY.MM.dd HH.mm.ss");
			String time = sdf.format(cal.getTime());
			ChunkStoriesLogger.init(new ChunkStoriesLogger(ChunkStoriesLogger.LogLevel.ALL, ChunkStoriesLogger.LogLevel.ALL, new File(GameDirectory.getGameFolderPath() + "/serverlogs/" + time + ".log")));
			GameData.reload();
			// Load world
			String worldName = serverConfig.getProp("world", "world");
			String worldDir = GameDirectory.getGameFolderPath() + "/worlds/" + worldName;
			if (new File(worldDir).exists())
			{
				world = new WorldServer(worldDir);
				world.startLogic();
			}
			else
			{
				System.out.println("Can't find the world \"" + worldName + "\" in " + worldDir + ". Exiting !");
				Runtime.getRuntime().exit(0);
			}
			// init multiverse
			announcer = new ServerAnnouncerThread();
			announcer.init();
			announcer.start();
			// load users privs
			UsersPrivileges.load();
			// init network
			handler.start();
			// Load plugins
			pluginsManager = new PluginsManager(this);
			pluginsManager.enablePlugins();
		}
		catch (Exception e)
		{ // Exceptions stuff
			log.severe("Could not initalize server. Stacktrace below");
			e.printStackTrace();
			System.exit(-1);
		}
		Scanner in = new Scanner(System.in);
		while (running.get())
		{ // main loop
			System.out.print("> ");
			String cmd = in.nextLine(); // Wait for input
			if (cmd != null)
			{
				try
				{
					ServerConsole.handleCommand(cmd, this); // Process it
				}
				catch (Exception e)
				{
					System.out.println("error while handling command :");
					e.printStackTrace();
				}
			}
		}
		in.close();
		pluginsManager.disablePlugins();
		closeServer();
		ChunkStoriesLogger.getInstance().save();
	}
	
	@Override
	public PluginsManager getPluginsManager()
	{
		return pluginsManager;
	}
	
	private void closeServer()
	{
		// When stopped, close sockets and save config.
		world.save();
		handler.closeAll();
		serverConfig.save();
		handler.close();
		UsersPrivileges.save();
		log.info("Good night sweet prince");
		Runtime.getRuntime().exit(0);
	}

	// log
	private void initLog()
	{
		// Dirty class for having proper log formatting.
		Handler h = new ConsoleHandler();
		h.setFormatter(new LogFormatter());
		for (Handler iHandler : log.getParent().getHandlers())
		{
			log.getParent().removeHandler(iHandler);
		}
		log.addHandler(h);
	}

	public void stop()
	{
		// When stopped, close sockets and save config.
		announcer.flagStop();
		running.set(false);
	}

	public boolean isRunning()
	{
		return running.get();
	}

	// Config shit
	public void reloadConfig()
	{
		UsersPrivileges.load();
		serverConfig.load();
	}

	@Override
	public Set<Player> getConnectedPlayers()
	{
		Set<Player> set = new HashSet<Player>();
		if (handler != null)
			for (ServerClient c : handler.getAuthentificatedClients())
				if (c.profile != null)
					set.add(c.profile);
		return set;
	}

	@Override
	public String getName()
	{
		return "Server console " + VersionInfo.version;
	}

	@Override
	public void sendMessage(String msg)
	{
		System.out.println(ColorsTools.convertToAnsi("#FF00FF" + msg));
	}

	@Override
	public boolean hasPermission(String permissionNode)
	{
		return true;
	}

	@Override
	public Player getPlayer(String string)
	{
		if (handler != null)
			for (ServerClient c : handler.getAuthentificatedClients())
				if (c.profile != null)
					if(c.profile.getName().startsWith(string))
						return c.profile;
		return null;
	}
}
