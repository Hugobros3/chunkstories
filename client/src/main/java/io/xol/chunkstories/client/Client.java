package io.xol.chunkstories.client;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import io.xol.engine.base.GameWindowOpenGL_LWJGL3;
import io.xol.engine.concurrency.SimpleFence;
import io.xol.engine.misc.ConfigFile;
import io.xol.engine.misc.NativesLoader;
import io.xol.chunkstories.Constants;
import io.xol.chunkstories.VersionInfo;
import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.client.ClientRenderingConfig;
import io.xol.chunkstories.api.client.ClientSoundManager;
import io.xol.chunkstories.api.item.inventory.Inventory;
import io.xol.chunkstories.api.particles.ParticlesManager;
import io.xol.chunkstories.api.player.PlayerClient;
import io.xol.chunkstories.api.plugin.ClientPluginManager;
import io.xol.chunkstories.api.rendering.effects.DecalsManager;
import io.xol.chunkstories.api.util.ChunkStoriesLogger;
import io.xol.chunkstories.api.util.ChunkStoriesLogger.LogLevel;
import io.xol.chunkstories.api.util.ConfigDeprecated;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.content.GameDirectory;
import io.xol.chunkstories.content.ClientGameContent;
import io.xol.chunkstories.gui.Ingame;
import io.xol.chunkstories.gui.MainMenu;
import io.xol.chunkstories.gui.overlays.LoginOverlay;
import io.xol.chunkstories.gui.overlays.MainMenuOverlay;
import io.xol.chunkstories.gui.overlays.general.MessageBoxOverlay;
import io.xol.chunkstories.gui.overlays.ingame.ConnectionOverlay;
import io.xol.chunkstories.gui.overlays.ingame.InventoryOverlay;
import io.xol.chunkstories.input.lwjgl2.Lwjgl3ClientInputsManager;
import io.xol.chunkstories.tools.ChunkStoriesLoggerImplementation;
import io.xol.chunkstories.tools.DebugProfiler;
import io.xol.chunkstories.world.WorldClientCommon;

public class Client implements ClientInterface
{
	public static ConfigDeprecated clientConfig = new ConfigFile("./config/client.cfg");

	//public static Lwjgl2ClientInputsManager inputsManager;
	//private final Lwjgl3ClientInputsManager inputsManager;

	public static boolean offline = false;

	private GameWindowOpenGL_LWJGL3 gameWindows;
	private final static RenderingConfig renderingConfig = new RenderingConfig();
	public static WorldClientCommon world;
	//public static GameLogicThread worldThread;

	public static String username = "Unknow";
	public static String session_key = "";

	public static DebugProfiler profiler = new DebugProfiler();

	private ClientGameContent gameContent;
	private PlayerClient clientSideController;

	private ChunkStoriesLoggerImplementation logger;
	//public ClientPluginManager pluginsManager;

	//public EntityControllable controlledEntity;
	public static Client staticClientReference;

	public static void main(String[] args)
	{
		// Check for folder
		GameDirectory.check();
		GameDirectory.initClientPath();

		String modsStringArgument = null;
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
				modsStringArgument = s.replace("--mods=", "");
			}
			else if (s.contains("--dir"))
			{
				GameDirectory.set(s.replace("--dir=", ""));
			}
			else
			{
				System.out.println("Chunk Stories arguments : \n" + "-oldgl Disables OpenGL 3.0+ stuff\n" + "-forceobsolete Forces the game to run even if requirements aren't met\n"
						+ "-mods=xxx,yyy | -mods=* Tells the game to start with those mods enabled\n" + "-dir=whatever Tells the game not to look for .chunkstories at it's normal location and instead use the argument" + "" + "");
			}
		}
		
		new Client(modsStringArgument);

		System.exit(-1);
	}

	public Client(String modsStringArgument)
	{
		staticClientReference = this;
		
		//Name the thread
		Thread.currentThread().setName("Main OpenGL Rendering thread");
		Thread.currentThread().setPriority(Constants.MAIN_GL_THREAD_PRIORITY);
		
		// Start logs
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("YYYY.MM.dd HH.mm.ss");
		String time = sdf.format(cal.getTime());
		logger = new ChunkStoriesLoggerImplementation(this, LogLevel.ALL, LogLevel.ALL, new File(GameDirectory.getGameFolderPath() + "/logs/" + time + ".log"));
		
		// Load natives for LWJGL
		NativesLoader.load();
		
		// Create game content manager
		gameContent = new ClientGameContent(this, modsStringArgument);
		gameContent.reload();
		
		//Load the correct language
		String lang = clientConfig.getProp("language", "undefined");
		if(!lang.equals("undefined"))
			gameContent.localization().loadTranslation(lang);
		
		// Creates game window
		gameWindows = new GameWindowOpenGL_LWJGL3(this, "Chunk Stories " + VersionInfo.version, -1, -1);
		RenderingConfig.define();

		//Initlializes windows screen to main menu ( and ask for login )
		gameWindows.setLayer(new LoginOverlay(gameWindows, new MainMenu(gameWindows)));
		
		//Pass control to the windows for main game loop
		gameWindows.run();
	}

	public static Client getInstance()
	{
		return staticClientReference;
	}

	@Override
	public ClientSoundManager getSoundManager()
	{
		return gameWindows.getSoundEngine();
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

	public static ConfigDeprecated getConfig()
	{
		return clientConfig;
	}

	@Override
	public boolean hasFocus()
	{
		if (gameWindows.getLayer() instanceof Ingame)
			return ((Ingame) gameWindows.getLayer()).hasFocus();
		return false;
	}

	@Override
	public void reloadAssets()
	{
		SimpleFence waitForReload = new SimpleFence();

		if (gameWindows.isMainGLWindow())
		{
			gameContent.reload();
			gameWindows.getInputsManager().reload();
			gameWindows.getRenderingContext().getFontRenderer().reloadFonts();

			return;
		}

		gameWindows.queueTask(new Runnable()
		{
			@Override
			public void run()
			{
				//ModsManager.reload();
				gameContent.reload();
				gameWindows.getInputsManager().reload();
				gameWindows.getRenderingContext().getFontRenderer().reloadFonts();
				
				waitForReload.signal();
			}
		});

		waitForReload.traverse();
	}

	@Override
	public void printChat(String textToPrint)
	{
		if (gameWindows.getLayer() instanceof Ingame)
			((Ingame) gameWindows.getLayer()).chat.insert(textToPrint);
	}

	public void openInventories(Inventory... inventories)
	{
		if (gameWindows.getLayer().getRootLayer() instanceof Ingame)
		{
			Ingame gmp = (Ingame) gameWindows.getLayer().getRootLayer();

			gmp.focus(false);
			
			gameWindows.setLayer(new InventoryOverlay(gameWindows, gmp, inventories));
			
			/*if (otherInventory != null)
				gmp.changeOverlay(new InventoryOverlay(gmp, null, new Inventory[] { ((EntityWithInventory) this.getPlayer().getControlledEntity()).getInventory(), otherInventory }));
			else
				gmp.changeOverlay(new InventoryOverlay(gmp, null, new Inventory[] { ((EntityWithInventory) this.getPlayer().getControlledEntity()).getInventory() }));
			*/
		}
	}

	@Override
	public PlayerClient getPlayer()
	{
		return clientSideController;
	}

	@Override
	public WorldClient getWorld()
	{
		return world;
	}

	@Override
	public void changeWorld(WorldClient world2)
	{
		WorldClientCommon world = (WorldClientCommon)world2;
		
		gameWindows.queueTask(new Runnable()
		{
			@Override
			public void run()
			{
				//Setup the new world and make a controller for it
				Client.world = world;
				clientSideController = new ClientWorldController(Client.this, world);

				//Change the scene
				Ingame ingameScene = new Ingame(gameWindows, world);

				//We want to keep the connection overlay when getting into a server
				if (gameWindows.getLayer() instanceof ConnectionOverlay)
				{
					ConnectionOverlay overlay = (ConnectionOverlay) gameWindows.getLayer();
					//If that happen, we want this connection overlay to forget he was originated from a server browser or whatever
					overlay.setParentScene(ingameScene);
				}
				else
					gameWindows.setLayer(ingameScene);
				
				//Switch scene but keep the overlay
				//ingameScene.changeOverlay(overlay);
				
				//Start only the logic after all that
				world.startLogic();
			}
		});
	}

	@Override
	public void exitToMainMenu()
	{
		gameWindows.queueTask(new Runnable()
		{
			@Override
			public void run()
			{
				gameWindows.setLayer(new MainMenuOverlay(gameWindows, new MainMenu(gameWindows)));
				
				if (world != null)
				{
					Client.world.destroy();
					Client.world = null;
				}
				clientSideController = null;
			}
		});
	}

	public void exitToMainMenu(String errorMessage)
	{
		gameWindows.queueTask(new Runnable()
		{
			@Override
			public void run()
			{
				gameWindows.setLayer(new MessageBoxOverlay(gameWindows, new MainMenu(gameWindows), errorMessage));
				
				if (world != null)
				{
					Client.world.destroy();
					Client.world = null;
				}
				clientSideController = null;
			}
		});
	}

	@Override
	public void print(String message)
	{
		ChunkStoriesLoggerImplementation.getInstance().info(message);
		printChat(message);
	}

	/*@Override
	public String getName()
	{
		return username;
	}

	@Override
	public void sendMessage(String msg)
	{
		print(msg);
	}

	@Override
	public boolean hasPermission(String permissionNode)
	{
		return true;
	}*/

	@Override
	public ClientGameContent getContent()
	{
		return gameContent;
	}

	private ClientPluginManager pluginManager = null;
	
	//We have to set a reference from Ingame via a callback since stuff called from within it's very constructor rely on this global reference.
	//TODO it shouldn't I guess ?
	public void setClientPluginManager(ClientPluginManager pl)
	{
		this.pluginManager = pl;
	}
	
	@Override
	public ClientPluginManager getPluginManager()
	{
		//if (windows.getCurrentScene() instanceof Ingame)
		//	return ((Ingame) windows.getCurrentScene()).getPluginManager();
		return pluginManager;
	}
	
	@Override
	public Lwjgl3ClientInputsManager getInputsManager()
	{
		//if (windows.getCurrentScene() instanceof Ingame)
		//	return ((Ingame) windows.getCurrentScene()).getInputsManager();
		return gameWindows.getInputsManager();
	}

	public GameWindowOpenGL_LWJGL3 getGameWindow()
	{
		return gameWindows;
	}

	@Override
	public ChunkStoriesLogger logger() {
		return this.logger;
	}

	@Override
	public String username() {
		return this.username;
	}

	@Override
	public ConfigDeprecated configDeprecated() {
		return this.clientConfig;
	}

	@Override
	public ClientRenderingConfig renderingConfig() {
		return renderingConfig;
	}
}
