package io.xol.chunkstories.server;

import static org.fusesource.jansi.Ansi.ansi;
import static org.fusesource.jansi.Ansi.Color.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

import io.xol.engine.misc.ColorsTools;
import io.xol.engine.misc.ConfigFile;
import io.xol.chunkstories.VersionInfo;
import io.xol.chunkstories.api.plugin.commands.Command;
import io.xol.chunkstories.api.server.Player;
import io.xol.chunkstories.api.server.ServerInterface;
import io.xol.chunkstories.content.GameData;
import io.xol.chunkstories.content.GameDirectory;
import io.xol.chunkstories.content.PluginsManager;
import io.xol.chunkstories.content.sandbox.GameLogicThread;
import io.xol.chunkstories.content.sandbox.UnthrustedUserContentSecurityManager;
import io.xol.chunkstories.server.net.ServerAnnouncerThread;
import io.xol.chunkstories.server.net.ServerClient;
import io.xol.chunkstories.server.net.ServerConnectionsManager;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.chunkstories.world.WorldServer;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class Server implements Runnable, ServerInterface
{
	// The server class handles and make the link between all server components
	// It also takes care of the command line input as it's the main thread,
	// thought the processing of command lines is handled by ServerConsole.java
	static Server server;

	public static void main(String a[])
	{
		server = new Server();
		server.run();
	}

	public static Server getInstance()
	{
		return server;
	}

	private ChunkStoriesLogger log = null;
	private ConfigFile serverConfig = new ConfigFile("./config/server.cfg");

	private AtomicBoolean running = new AtomicBoolean(true);
	private long initTimestamp = System.currentTimeMillis() / 1000;

	private WorldServer world;
	private GameLogicThread worldThread;

	private ServerConnectionsManager connectionsManager;
	private ServerConsole console = new ServerConsole(this);

	private PluginsManager pluginsManager;

	// Sleeper thread to keep servers list updated
	private ServerAnnouncerThread announcer;

	@Override
	public void run()
	{
		// Start server services
		try
		{
			//Init logs first !
			Calendar cal = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("YYYY.MM.dd HH.mm.ss");
			String time = sdf.format(cal.getTime());

			ChunkStoriesLogger.init(new ChunkStoriesLogger(ChunkStoriesLogger.LogLevel.ALL, ChunkStoriesLogger.LogLevel.ALL, new File(GameDirectory.getGameFolderPath() + "/serverlogs/" + time + ".log")));
			log = ChunkStoriesLogger.getInstance();

			log.info("Starting ChunkStories server " + VersionInfo.version + " network protocol v" + VersionInfo.networkProtocolVersion);
			connectionsManager = new ServerConnectionsManager(this);

			GameData.reload();
			// Load world
			String worldName = serverConfig.getProp("world", "world");
			String worldDir = GameDirectory.getGameFolderPath() + "/worlds/" + worldName;
			if (new File(worldDir).exists())
			{
				world = new WorldServer(this, worldDir);
				worldThread = new GameLogicThread(world, new UnthrustedUserContentSecurityManager());
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
			connectionsManager.start();
			// Load plugins
			pluginsManager = new PluginsManager(this);
		}
		catch (Exception e)
		{ // Exceptions stuff
			log.error("Could not initalize server. Stacktrace below");
			e.printStackTrace();
			System.exit(-1);
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.print("> ");
		while (running.get())
		{ // main loop
			try
			{
				// wait until we have data to complete a readLine()
				while (!br.ready() && running.get())
				{
					printTopScreenDebug();
					
					Thread.sleep(1000L);
				}
				if (!running.get())
					break;
				String unparsedCommandText = br.readLine();
				if (unparsedCommandText == null)
					continue;
				try
				{
					//Parse and fire
					String cmdName = unparsedCommandText.toLowerCase();
					String[] args = {};
					if (unparsedCommandText.contains(" "))
					{
						cmdName = unparsedCommandText.substring(0, unparsedCommandText.indexOf(" "));
						args = unparsedCommandText.substring(unparsedCommandText.indexOf(" ") + 1, unparsedCommandText.length()).split(" ");
					}

					console.handleCommand(this, new Command(cmdName), args);

					System.out.print("> ");
					System.out.flush();
				}
				catch (Exception e)
				{
					System.out.println("error while handling command :");
					e.printStackTrace();
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			catch (InterruptedException e)
			{
				System.out.println("ConsoleInputReadTask() cancelled");
				break;
			}

		}
		try
		{
			br.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		closeServer();
		ChunkStoriesLogger.getInstance().save();
	}

	private void printTopScreenDebug()
	{
		String txt = "" + ansi().fg(BLACK).bg(CYAN);
		
		txt += "Chunk Stories Server " + VersionInfo.version;
		txt += " | world running at " + world.getGameLogic().getSimulationFps()+" Fps";
		txt += " | " + this.connectionsManager.getNumberOfAuthentificatedClients() + "/" + this.connectionsManager.getMaxClients() + " players";
		txt += " | " + this.world.getRegionsHolder().getStats() + " + " + this.world.getRegionSummaries().countSummaries() + " summaries ";
		txt += " | " + this.world.ioHandler.toString();
		
		txt += ansi().bg(BLACK).fg(WHITE);
		
		System.out.print(ansi().saveCursorPosition().cursor(0, 0).eraseLine().fg(RED) + txt + ansi().restoreCursorPosition());
		System.out.flush();
	}

	public WorldServer getWorld()
	{
		return world;
	}

	public ConfigFile getServerConfig()
	{
		return serverConfig;
	}

	@Override
	public PluginsManager getPluginsManager()
	{
		return pluginsManager;
	}

	private void closeServer()
	{
		// When stopped, close sockets and save config.
		log.info("Killing all connections");
		connectionsManager.closeAll();
		connectionsManager.closeConnection();
		
		log.info("Saving map ...");
		world.saveEverything();
		log.info("Shutting down plugins ...");
		pluginsManager.disablePlugins();
		log.info("Done, shutting down threads ... ");
		worldThread.stopLogicThread();
		log.info("Game logic done");
		world.ioHandler.shutdown();
		log.info("IO done");
		
		log.info("Saving configuration");
		serverConfig.save();
		UsersPrivileges.save();
		log.info("Good night sweet prince");
		Runtime.getRuntime().exit(0);
	}

	public void stop()
	{
		// When stopped, close sockets and save config.
		announcer.stopAnnouncer();
		running.set(false);
	}

	public boolean isRunning()
	{
		return running.get();
	}

	public void reloadConfig()
	{
		UsersPrivileges.load();
		serverConfig.load();
	}

	@Override
	public Iterator<Player> getConnectedPlayers()
	{
		return new Iterator<Player>()
		{
			Iterator<ServerClient> authClients = connectionsManager.getAuthentificatedClients();

			@Override
			public boolean hasNext()
			{
				return authClients.hasNext();
			}

			@Override
			public Player next()
			{
				return authClients.next().getProfile();
			}

		};
	}

	@Override
	public String getName()
	{
		return "Server console " + VersionInfo.version + "";
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
	public Player getPlayer(String playerName)
	{
		ServerClient clientByThatName = connectionsManager.getAuthentificatedClientByName(playerName);
		if (clientByThatName != null)
			return clientByThatName.getProfile();
		return null;
	}

	@Override
	public Player getPlayerByUUID(long UUID)
	{
		Iterator<Player> i = getConnectedPlayers();
		while (i.hasNext())
		{
			Player p = i.next();
			if (p.getUUID() == UUID)
				return p;
		}
		return null;
	}

	public ServerConsole getConsole()
	{
		return console;
	}

	public ServerConnectionsManager getHandler()
	{
		return connectionsManager;
	}

	public ChunkStoriesLogger getLogger()
	{
		return log;
	}

	public long getUptime()
	{
		return (System.currentTimeMillis() / 1000 - initTimestamp);
	}
}
