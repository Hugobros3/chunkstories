package io.xol.chunkstories.client;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.misc.ConfigFile;
import io.xol.engine.misc.IconLoader;
import io.xol.engine.misc.NativesLoader;

import io.xol.chunkstories.VersionInfo;
import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.entity.ClientSideController;
import io.xol.chunkstories.api.entity.EntityInventory;
import io.xol.chunkstories.api.entity.interfaces.EntityWithInventory;
import io.xol.chunkstories.api.input.InputsManager;
import io.xol.chunkstories.api.particles.ParticlesManager;
import io.xol.chunkstories.api.rendering.DecalsManager;
import io.xol.chunkstories.api.sound.SoundManager;
import io.xol.chunkstories.client.net.ClientToServerConnection;
import io.xol.chunkstories.content.GameData;
import io.xol.chunkstories.content.GameDirectory;
import io.xol.chunkstories.content.PluginsManager;
import io.xol.chunkstories.content.sandbox.GameLogicThread;
import io.xol.chunkstories.gui.GameplayScene;
import io.xol.chunkstories.gui.MainMenu;
import io.xol.chunkstories.gui.menus.InventoryOverlay;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.chunkstories.tools.DebugProfiler;
import io.xol.chunkstories.world.WorldClientCommon;

public class Client implements /*ClientSideController, */ClientInterface
{
	public static ConfigFile clientConfig = new ConfigFile("./config/client.cfg");

	public static ClientInputsManager inputsManager;

	public static boolean offline = false;

	public static ClientToServerConnection connection;
	public static GameWindowOpenGL windows;
	public static WorldClientCommon world;
	public static GameLogicThread worldThread;

	public static String username = "Unknow";
	public static String session_key = "nopeMLG";

	public static DebugProfiler profiler = new DebugProfiler();

	private ClientSideController clientSideController;

	//public EntityControllable controlledEntity;
	public static Client clientController;

	public PluginsManager pluginsManager;

	public static void main(String[] args)
	{
		// Check for folder
		GameDirectory.check();
		GameDirectory.initClientPath();

		for (String s : args) // Debug arguments
		{
			if (s.equals("-oldgl"))
			{
				RenderingConfig.openGL3Capable = false;
				System.out.println("Legacy OpenGL mode enabled");
			}
			else if (s.equals("-forceobsolete"))
			{
				RenderingConfig.ignoreObsoleteHardware = false;
				System.out.println("Legacy OpenGL mode enabled");
			}
			else if (s.contains("-mods"))
			{
				String[] modsString = s.replace("-mods=", "").split(",");
				GameData.setEnabledMods(modsString);
			}
			else if (s.contains("-dir"))
			{
				GameDirectory.set(s.replace("-dir=", ""));
			}
			else
			{
				System.out.println("Chunk Stories arguments : \n" + "-oldgl Disables OpenGL 3.0+ stuff\n" + "-forceobsolete Forces the game to run even if requirements aren't met\n"
						+ "-mods=xxx,yyy | -mods=* Tells the game to start with those mods enabled\n" + "-dir=whatever Tells the game not to look for .chunkstories at it's normal location and instead use the argument" + "" + "");

				//Runtime.getRuntime().exit(0);
			}
		}
		new Client();
	}

	public Client()
	{
		clientController = this;
		// Start logs
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("YYYY.MM.dd HH.mm.ss");
		String time = sdf.format(cal.getTime());
		ChunkStoriesLogger.init(new ChunkStoriesLogger(ChunkStoriesLogger.LogLevel.ALL, ChunkStoriesLogger.LogLevel.ALL, new File(GameDirectory.getGameFolderPath() + "/logs/" + time + ".log")));
		// Load natives
		RenderingConfig.define();
		NativesLoader.load();
		// Load last gamemode
		GameData.reload();
		inputsManager = new ClientInputsManager();
		// Gl init
		windows = new GameWindowOpenGL(this, "Chunk Stories " + VersionInfo.version, -1, -1);
		windows.createContext();

		GameData.reloadClientContent();
		windows.changeScene(new MainMenu(windows, true));
		//Load 
		pluginsManager = new PluginsManager(clientController);
		windows.run();
	}

	public static Client getInstance()
	{
		return clientController;
	}

	public static void onStart()
	{
		IconLoader.load();
	}

	@Override
	public SoundManager getSoundManager()
	{
		return windows.getSoundEngine();
	}

	@Override
	public InputsManager getInputsManager()
	{
		return inputsManager;
	}

	@Override
	public ParticlesManager getParticlesManager()
	{
		return world.getParticlesManager();
	}

	@Override
	public DecalsManager getDecalsManager()
	{
		return world.getDecalsManager();
	}

	public static void onClose()
	{
		clientConfig.save();
	}

	public static ConfigFile getConfig()
	{
		return clientConfig;
	}

	@Override
	public boolean hasFocus()
	{
		if (windows.getCurrentScene() instanceof GameplayScene)
			return ((GameplayScene) windows.getCurrentScene()).hasFocus();
		return false;
	}

	@Override
	public PluginsManager getPluginsManager()
	{
		return pluginsManager;
	}

	@Override
	public void reloadAssets()
	{
		GameData.reload();
		inputsManager.reload();
		GameData.reloadClientContent();
	}

	@Override
	public void printChat(String textToPrint)
	{
		if (windows.getCurrentScene() instanceof GameplayScene)
			((GameplayScene) windows.getCurrentScene()).chat.insert(textToPrint);
	}

	@Override
	public ClientToServerConnection getServerConnection()
	{
		return connection;
	}

	public void openInventory(EntityInventory otherInventory)
	{
		if (windows.getCurrentScene() instanceof GameplayScene)
		{
			GameplayScene gmp = (GameplayScene) windows.getCurrentScene();

			gmp.focus(false);
			if (otherInventory != null)
				gmp.changeOverlay(new InventoryOverlay(gmp, null, new EntityInventory[] { ((EntityWithInventory) this.getClientSideController().getControlledEntity()).getInventory(), otherInventory }));
			else
				gmp.changeOverlay(new InventoryOverlay(gmp, null, new EntityInventory[] { ((EntityWithInventory) this.getClientSideController().getControlledEntity()).getInventory() }));
		}
	}

	@Override
	public ClientSideController getClientSideController()
	{
		return clientSideController;
	}

	@Override
	public void changeWorld(WorldClientCommon world)
	{
		windows.queueTask(new Runnable()
		{
			@Override
			public void run()
			{
				Client.world = world;
				clientSideController = new ClientWorldController(Client.this, world);

				Client.windows.changeScene(new GameplayScene(windows, false));
			}
		});
	}

	@Override
	public void exitToMainMenu()
	{
		windows.queueTask(new Runnable()
		{
			@Override
			public void run()
			{
				Client.world.destroy();
				Client.worldThread.stopLogicThread();
				
				Client.world = null;
				clientSideController = null;

				Client.windows.changeScene(new MainMenu(windows, false));
			}
		});
	}
}
