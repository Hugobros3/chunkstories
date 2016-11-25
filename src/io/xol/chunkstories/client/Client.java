package io.xol.chunkstories.client;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.concurrency.SimpleFence;
import io.xol.engine.misc.ConfigFile;
import io.xol.engine.misc.IconLoader;
import io.xol.engine.misc.NativesLoader;
import io.xol.chunkstories.Constants;
import io.xol.chunkstories.VersionInfo;
import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.entity.ClientSideController;
import io.xol.chunkstories.api.entity.Inventory;
import io.xol.chunkstories.api.entity.interfaces.EntityWithInventory;
import io.xol.chunkstories.api.particles.ParticlesManager;
import io.xol.chunkstories.api.rendering.effects.DecalsManager;
import io.xol.chunkstories.api.sound.SoundManager;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.content.GameDirectory;
import io.xol.chunkstories.content.ModsManager;
import io.xol.chunkstories.content.PluginsManager;
import io.xol.chunkstories.gui.Ingame;
import io.xol.chunkstories.gui.MainMenu;
import io.xol.chunkstories.gui.OverlayableScene;
import io.xol.chunkstories.gui.overlays.ingame.ConnectionOverlay;
import io.xol.chunkstories.gui.overlays.ingame.InventoryOverlay;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.chunkstories.tools.DebugProfiler;
import io.xol.chunkstories.world.WorldClientCommon;

public class Client implements ClientInterface
{
	public static ConfigFile clientConfig = new ConfigFile("./config/client.cfg");

	public static ClientInputsManager inputsManager;

	public static boolean offline = false;

	//public static ClientToServerConnection connection;
	public static GameWindowOpenGL windows;
	public static WorldClientCommon world;
	//public static GameLogicThread worldThread;

	public static String username = "Unknow";
	public static String session_key = "";

	public static DebugProfiler profiler = new DebugProfiler();

	private ClientSideController clientSideController;

	//public EntityControllable controlledEntity;
	public static Client client;

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
				RenderingConfig.gl_openGL3Capable = false;
				System.out.println("Legacy OpenGL mode enabled");
			}
			else if (s.equals("-forceobsolete"))
			{
				RenderingConfig.ignoreObsoleteHardware = false;
				System.out.println("Legacy OpenGL mode enabled");
			}
			else if (s.contains("--mods"))
			{
				String[] modsString = s.replace("--mods=", "").split(",");
				ModsManager.setEnabledMods(modsString);
			}
			else if (s.contains("--dir"))
			{
				GameDirectory.set(s.replace("--dir=", ""));
			}
			else
			{
				System.out.println("Chunk Stories arguments : \n" + "-oldgl Disables OpenGL 3.0+ stuff\n" + "-forceobsolete Forces the game to run even if requirements aren't met\n"
						+ "-mods=xxx,yyy | -mods=* Tells the game to start with those mods enabled\n" + "-dir=whatever Tells the game not to look for .chunkstories at it's normal location and instead use the argument" + "" + "");

				//Runtime.getRuntime().exit(0);
			}
		}

		Thread.currentThread().setName("Main OpenGL Rendering thread");
		Thread.currentThread().setPriority(Constants.MAIN_GL_THREAD_PRIORITY);
		new Client();
	}

	public Client()
	{
		client = this;
		// Start logs
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("YYYY.MM.dd HH.mm.ss");
		String time = sdf.format(cal.getTime());
		ChunkStoriesLogger.init(new ChunkStoriesLogger(ChunkStoriesLogger.LogLevel.ALL, ChunkStoriesLogger.LogLevel.ALL, new File(GameDirectory.getGameFolderPath() + "/logs/" + time + ".log")));
		// Load natives
		RenderingConfig.define();
		NativesLoader.load();
		// Load last gamemode
		ModsManager.reload();
		inputsManager = new ClientInputsManager();
		// Gl init
		windows = new GameWindowOpenGL(this, "Chunk Stories " + VersionInfo.version, -1, -1);
		windows.createContext();

		ModsManager.reloadClientContent();
		windows.changeScene(new MainMenu(windows, true));
		//Load 
		pluginsManager = new PluginsManager(client);
		windows.run();
	}

	public static Client getInstance()
	{
		return client;
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
	public ClientInputsManager getInputsManager()
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
		if (windows.getCurrentScene() instanceof Ingame)
			return ((Ingame) windows.getCurrentScene()).hasFocus();
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
		SimpleFence waitForReload = new SimpleFence();
		
		if(GameWindowOpenGL.isMainGLWindow())
		{
			ModsManager.reload();
			inputsManager.reload();
			ModsManager.reloadClientContent();
			
			return;
		}
		
		windows.queueTask(new Runnable()
		{
			@Override
			public void run()
			{
				ModsManager.reload();
				inputsManager.reload();
				ModsManager.reloadClientContent();
				
				waitForReload.signal();
			}
		});
		
		waitForReload.traverse();
	}

	@Override
	public void printChat(String textToPrint)
	{
		if (windows.getCurrentScene() instanceof Ingame)
			((Ingame) windows.getCurrentScene()).chat.insert(textToPrint);
	}

	public void openInventory(Inventory otherInventory)
	{
		if (windows.getCurrentScene() instanceof Ingame)
		{
			Ingame gmp = (Ingame) windows.getCurrentScene();

			gmp.focus(false);
			if (otherInventory != null)
				gmp.changeOverlay(new InventoryOverlay(gmp, null, new Inventory[] { ((EntityWithInventory) this.getClientSideController().getControlledEntity()).getInventory(), otherInventory }));
			else
				gmp.changeOverlay(new InventoryOverlay(gmp, null, new Inventory[] { ((EntityWithInventory) this.getClientSideController().getControlledEntity()).getInventory() }));
		}
	}

	@Override
	public ClientSideController getClientSideController()
	{
		return clientSideController;
	}

	@Override
	public WorldClient getWorld()
	{
		return world;
	}

	@Override
	public void changeWorld(WorldClientCommon world)
	{
		windows.queueTask(new Runnable()
		{
			@Override
			public void run()
			{
				//Setup the new world and make a controller for it
				Client.world = world;
				clientSideController = new ClientWorldController(Client.this, world);
				world.startLogic();

				//Change the scene
				Ingame ingameScene = new Ingame(windows, world);

				//We want to keep the connection overlay when getting into a server
				ConnectionOverlay overlay = null;
				if(Client.windows.getCurrentScene() instanceof OverlayableScene && ((OverlayableScene)Client.windows.getCurrentScene()).currentOverlay instanceof ConnectionOverlay)
				{
					overlay = (ConnectionOverlay) ((OverlayableScene)Client.windows.getCurrentScene()).currentOverlay;
					//If that happen, we want this connection overlay to forget he was originated from a server browser or whatever
					overlay.mainScene = ingameScene;
					overlay.parent = null;
				}
				
				ingameScene.changeOverlay(overlay);
				Client.windows.changeScene(ingameScene);
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
				if (world != null)
				{
					Client.world.destroy();
					Client.world = null;
				}
				clientSideController = null;

				Client.windows.changeScene(new MainMenu(windows, false));
			}
		});
	}

	public void exitToMainMenu(String errorMessage)
	{
		windows.queueTask(new Runnable()
		{
			@Override
			public void run()
			{
				if (world != null)
				{
					Client.world.destroy();
					Client.world = null;
				}
				clientSideController = null;

				Client.windows.changeScene(new MainMenu(windows, errorMessage));
			}
		});
	}
}
