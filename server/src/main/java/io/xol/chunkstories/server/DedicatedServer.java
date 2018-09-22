//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.server;

import static org.fusesource.jansi.Ansi.ansi;
import static org.fusesource.jansi.Ansi.Color.BLACK;
import static org.fusesource.jansi.Ansi.Color.CYAN;
import static org.fusesource.jansi.Ansi.Color.RED;
import static org.fusesource.jansi.Ansi.Color.WHITE;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

import io.xol.chunkstories.api.plugin.ServerPluginManager;
import io.xol.chunkstories.api.server.Server;
import io.xol.chunkstories.api.util.Configuration;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldInfo;
import io.xol.chunkstories.world.WorldInfoUtilKt;
import io.xol.chunkstories.world.WorldLoadingException;
import org.fusesource.jansi.AnsiConsole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.content.Content;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.server.PermissionsManager;
import io.xol.chunkstories.api.server.UserPrivileges;
import io.xol.chunkstories.api.util.ColorsTools;
import io.xol.chunkstories.api.util.IterableIterator;
import io.xol.chunkstories.api.workers.Tasks;
import io.xol.chunkstories.content.GameContentStore;
import io.xol.chunkstories.content.GameDirectory;
import io.xol.chunkstories.plugin.DefaultPluginManager;
import io.xol.chunkstories.server.commands.DedicatedServerConsole;
import io.xol.chunkstories.server.commands.InstallServerCommands;
import io.xol.chunkstories.server.net.ClientsManager;
import io.xol.chunkstories.server.net.announcer.ServerAnnouncerThread;
import io.xol.chunkstories.server.net.vanillasockets.VanillaClientsManager;
import io.xol.chunkstories.server.propagation.ServerModsProvider;
import io.xol.chunkstories.task.WorkerThreadPool;
import io.xol.chunkstories.util.LogbackSetupHelper;
import io.xol.chunkstories.util.VersionInfo;
import io.xol.chunkstories.world.WorldServer;

/**
 * The server class handles and make the link between all server components It
 * also takes care of the command line input as it's the main thread, thought
 * the processing of command lines is handled by ServerConsole.java
 */
public class DedicatedServer implements Runnable, Server {
	static DedicatedServer server;

	public static void main(String args[]) {
		File coreContentLocation = new File("core_content.zip");

		String modsString = null;
		for (String s : args) // Debug arguments
		{
			if (s.contains("--mods")) {
				modsString = s.replace("--mods=", "");
			} else if (s.contains("--dir")) {
				GameDirectory.set(s.replace("--dir=", ""));
			} else if (s.contains("--core")) {
				String coreContentLocationPath = s.replace("--core=", "");
				coreContentLocation = new File(coreContentLocationPath);
			} else {
				String helpText = "Chunk Stories server " + VersionInfo.version + "\n";

				if (s.equals("-h") || s.equals("--help"))
					helpText += "Command line help: \n";
				else
					helpText += "Unrecognized command: " + s + "\n";

				helpText += "--mods=xxx,yyy | -mods=* Tells the game to start with those mods enabled\n";
				helpText += "--dir=whatever Tells the game not to look for .chunkstories at it's normal location and instead use the argument\n";
				helpText += "--core=whaterverfolder/ or --core=whatever.zip Tells the game to use some specific folder or archive as it's base content.\n";

				System.out.println(helpText);
				return;

				// Runtime.getRuntime().exit(0);
			}
		}

		server = new DedicatedServer(coreContentLocation, modsString);

		server.run();
	}

	private final GameContentStore gameContent;
	private final WorkerThreadPool workers;

	private Logger logger = null;
	private DedicatedServerConsole console = new DedicatedServerConsole(this);

	private Configuration serverConfig = new Configuration(this);
	private final File configFile = new File("./config/server.config");

	private AtomicBoolean running = new AtomicBoolean(true);
	private long initTimestamp = System.currentTimeMillis() / 1000;

	private WorldServer world;

	private ClientsManager clientsManager;

	private FileBasedUsersPrivileges userPrivileges = new FileBasedUsersPrivileges();
	private PermissionsManager permissionsManager;

	// Sleeper thread to keep servers list updated
	private ServerAnnouncerThread announcer;

	// What mods are required to join this server ?
	private ServerModsProvider modsProvider;
	private DefaultServerPluginManager pluginsManager;

	DedicatedServer(File coreContentLocation, String modsString) {
		AnsiConsole.systemInstall();

		server = this;
		// Start server services
		try {
			// Initialize logs to a file bearing the current date
			Calendar cal = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("YYYY.MM.dd HH.mm.ss");
			String time = sdf.format(cal.getTime());

			logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

			String loggingFilename = GameDirectory.getGameFolderPath() + "/serverlogs/" + time + ".log";
			new LogbackSetupHelper(loggingFilename);

			logger.info("Starting Chunkstories server " + VersionInfo.version + " network protocol version "
					+ VersionInfo.networkProtocolVersion);

			// Loads the mods/build filesystem
			gameContent = new GameContentStore(this, coreContentLocation, modsString);
			gameContent.reload();

			// Spawns worker threads
			int nbThreads = this.serverConfig.getIntValue("server.performance.workerThreads");
			if (nbThreads <= 0) {
				nbThreads = Runtime.getRuntime().availableProcessors() - 2;

				// Fail-safe
				if (nbThreads < 1)
					nbThreads = 1;
			}

			workers = new WorkerThreadPool(nbThreads);
			workers.start();

			clientsManager = new VanillaClientsManager(this);

			modsProvider = new ServerModsProvider(this);

			// load users privs
			// UsersPrivilegesFile.load();
			pluginsManager = new DefaultServerPluginManager(this);

			// Load the world(s)
			String worldName = serverConfig.getValue("server.world");
			String worldPath = GameDirectory.getGameFolderPath() + "/worlds/" + worldName;
			File worldDir = new File(worldPath);
			if (worldDir.exists()) {
				File worldInfoFile = new File(worldDir.getPath()+"/worldInfo.dat");
				if(!worldInfoFile.exists())
					throw new WorldLoadingException("The folder $folder doesn't contain a worldInfo.dat file !");

				WorldInfo worldInfo = WorldInfoUtilKt.deserializeWorldInfo(worldInfoFile);

				world = new WorldServer(this, worldInfo, worldDir);
			} else {
				serverConfig.save(configFile);
				System.out.println("Can't find the world \"" + worldName + "\" in " + worldPath + ". Exiting !");
				Runtime.getRuntime().exit(0);
			}

			// Opens socket and starts accepting clients
			clientsManager.open();
			// Initializes the announcer ( server listings )
			announcer = new ServerAnnouncerThread(this);
			announcer.start();

			permissionsManager = (player, permissionNode) -> userPrivileges.isUserAdmin(player.getName());

			// Load plugins
			pluginsManager.reloadPlugins();
			new InstallServerCommands(this);

			// Finally start logic
			world.startLogic();
		} catch (Exception e) {
			logger.error("Could not initialize server . Stacktrace below");
			throw new RuntimeException(e);
		}
	}

	@Override
	// Just a command prompt, the actual server threads run in the background !
	public void run() {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.print("> ");
		while (running.get()) {
			try {
				// wait until we have data to complete a readLine()
				while (!br.ready() && running.get()) {
					printTopScreenDebug();

					Thread.sleep(1000L);
				}
				if (!running.get())
					break;
				String unparsedCommandText = br.readLine();
				if (unparsedCommandText == null)
					continue;
				try {
					// Parse and fire
					String cmdName = unparsedCommandText.toLowerCase();
					String[] args = {};
					if (unparsedCommandText.contains(" ")) {
						cmdName = unparsedCommandText.substring(0, unparsedCommandText.indexOf(" "));
						args = unparsedCommandText
								.substring(unparsedCommandText.indexOf(" ") + 1, unparsedCommandText.length())
								.split(" ");
					}

					console.dispatchCommand(console, cmdName, args);

					System.out.print("> ");
					System.out.flush();
				} catch (Exception e) {
					System.out.println("error while handling command :");
					e.printStackTrace();
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				System.out.println("ConsoleInputReadTask() cancelled");
				break;
			}

		}
		try {
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		closeServer();
	}

	private void printTopScreenDebug() {
		String txt = "" + ansi().fg(BLACK).bg(CYAN);

		int ec = 0;
		IterableIterator<Entity> i = world.getAllLoadedEntities();
		while (i.hasNext()) {
			i.next();
			ec++;
		}

		long maxRam = Runtime.getRuntime().maxMemory() / (1024 * 1024);
		long freeRam = Runtime.getRuntime().freeMemory() / (1024 * 1024);
		long usedRam = maxRam - freeRam;

		txt += "ChunkStories server " + VersionInfo.version;
		txt += " | fps:" + world.getGameLogic().getSimulationFps();
		txt += " | ent:" + ec;
		txt += " | players:" + this.clientsManager.getPlayersNumber() + "/" + this.clientsManager.getMaxClients();
		txt += " | lc:" + this.world.getRegionsHolder().getStats() + " ls:"
				+ this.world.getRegionsSummariesHolder().countSummaries();
		txt += " | ram:" + usedRam + "/" + maxRam;
		txt += " | " + this.workers.toShortString();
		txt += " | ioq:" + this.world.getIoHandler().getSize();

		txt += ansi().bg(BLACK).fg(WHITE);

		System.out.print(
				ansi().saveCursorPosition().cursor(0, 0).eraseLine().fg(RED) + txt + ansi().restoreCursorPosition());
		System.out.flush();
	}

	public WorldServer getWorld() {
		return world;
	}

	public Configuration getServerConfig() {
		return serverConfig;
	}

	@Override
	public ServerPluginManager getPluginManager() {
		return pluginsManager;
	}

	private void closeServer() {
		// When stopped, close sockets and save config.
		logger.info("Stopping world logic");

		logger.info("Killing all connections");
		clientsManager.close();

		logger.info("Shutting down plugins ...");
		pluginsManager.disablePlugins();

		logger.info("Saving map and waiting for IO to finish");
		world.saveEverything();
		world.getIoHandler().waitThenKill();

		logger.info("Done");
		world.destroy();

		logger.info("Saving configuration");
		serverConfig.save(configFile);
		userPrivileges.save();
		logger.info("Good night sweet prince");
		Runtime.getRuntime().exit(0);
	}

	public void stop() {
		announcer.stopAnnouncer();
		workers.destroy();

		// When stopped, close sockets and save config.
		running.set(false);
	}

	public boolean isRunning() {
		return running.get();
	}

	public void reloadConfig() {
		userPrivileges.load();
		serverConfig.load(configFile);
	}

	@Override
	public IterableIterator<Player> getConnectedPlayers() {
		return clientsManager.getPlayers();
	}

	@Override
	public int getConnectedPlayersCount() {
		return clientsManager.getPlayersNumber();
	}

	@Override
	public String toString() {
		return "[ChunkStoriesServer " + VersionInfo.version + "]";
	}

	@Override
	public Player getPlayerByName(String playerName) {
		return clientsManager.getPlayerByName(playerName);
	}

	@Override
	public Player getPlayerByUUID(long UUID) {
		Iterator<Player> i = getConnectedPlayers();
		while (i.hasNext()) {
			Player p = i.next();
			if (p.getUUID() == UUID)
				return p;
		}
		return null;
	}

	public DedicatedServerConsole getConsole() {
		return console;
	}

	public ClientsManager getHandler() {
		return clientsManager;
	}

	public Logger getLogger() {
		return logger;
	}

	public long getUptime() {
		return (System.currentTimeMillis() / 1000 - initTimestamp);
	}

	@Override
	public void broadcastMessage(String message) {
		server.logger().info(ColorsTools.convertToAnsi(message));
		for (Player player : getConnectedPlayers()) {
			player.sendMessage(message);
		}
	}

	/**
	 * Returns the mods provider
	 */
	public ServerModsProvider getModsProvider() {
		return modsProvider;
	}

	@Override
	public void print(String message) {
		logger.info(message);
	}

	@Override
	public Content getContent() {
		return gameContent;
	}

	@Override
	public PermissionsManager getPermissionsManager() {
		return permissionsManager;
	}

	@Override
	public void setPermissionsManager(PermissionsManager permissionsManager) {
		this.permissionsManager = permissionsManager;
	}

	@Override
	public Logger logger() {
		return logger;
	}

	@Override
	/** Dedicated servers openly broadcast their public IP */
	public String getPublicIp() {
		return this.clientsManager.getIP();
	}

	@Override
	public UserPrivileges getUserPrivileges() {
		return userPrivileges;
	}

	@Override
	public Tasks getTasks() {
		return workers;
	}
}
