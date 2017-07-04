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
import io.xol.chunkstories.Constants;
import io.xol.chunkstories.VersionInfo;
import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.client.ClientRenderingConfig;
import io.xol.chunkstories.api.client.ClientSoundManager;
import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.item.inventory.Inventory;
import io.xol.chunkstories.api.particles.ParticlesManager;
import io.xol.chunkstories.api.player.PlayerClient;
import io.xol.chunkstories.api.plugin.ClientPluginManager;
import io.xol.chunkstories.api.rendering.effects.DecalsManager;
import io.xol.chunkstories.api.util.ChunkStoriesLogger;
import io.xol.chunkstories.api.util.ChunkStoriesLogger.LogLevel;
import io.xol.chunkstories.api.util.concurrency.Fence;
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
import io.xol.chunkstories.input.lwjgl3.Lwjgl3ClientInputsManager;
import io.xol.chunkstories.tools.ChunkStoriesLoggerImplementation;
import io.xol.chunkstories.tools.DebugProfiler;
import io.xol.chunkstories.world.WorldClientCommon;

public class Client implements ClientInterface
{
	private static Client staticClientReference; //Self-reference for static access
	
	//Base client data
	private final ConfigDeprecated clientConfig = new ConfigFile("./config/client.cfg");
	private final ChunkStoriesLoggerImplementation logger;
	private final ClientGameContent gameContent;

	//Windowing/Rendering
	private final GameWindowOpenGL_LWJGL3 gameWindow;
	private final RenderingConfig renderingConfig = new RenderingConfig();
	
	//Login data
	public static String username = "Unknow";
	public static String session_key = "";
	public static boolean offline = false;
	
	//Gameplay data
	private WorldClientCommon world;
	private PlayerClient clientSideController;

	//Debug
	public static DebugProfiler profiler = new DebugProfiler();

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
		
		// Create game content manager
		gameContent = new ClientGameContent(this, modsStringArgument);
		gameContent.reload();
		
		//Load the correct language
		String lang = clientConfig.getProp("language", "undefined");
		if(!lang.equals("undefined"))
			gameContent.localization().loadTranslation(lang);
		
		// Creates game window
		gameWindow = new GameWindowOpenGL_LWJGL3(this, "Chunk Stories " + VersionInfo.version, -1, -1);
		RenderingConfig.define();

		//Initlializes windows screen to main menu ( and ask for login )
		gameWindow.setLayer(new LoginOverlay(gameWindow, new MainMenu(gameWindow)));
		
		//Pass control to the windows for main game loop
		gameWindow.run();
	}

	public static Client getInstance()
	{
		return staticClientReference;
	}

	@Override
	public ClientSoundManager getSoundManager()
	{
		return gameWindow.getSoundEngine();
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

	public void onClose()
	{
		clientConfig.save();
	}

	public ConfigDeprecated getConfig()
	{
		return clientConfig;
	}

	@Override
	public boolean hasFocus()
	{
		if (gameWindow.getLayer() instanceof Ingame)
			return ((Ingame) gameWindow.getLayer()).hasFocus();
		return false;
	}

	@Override
	public void reloadAssets()
	{
		SimpleFence waitForReload = new SimpleFence();

		if (gameWindow.isMainGLWindow())
		{
			gameContent.reload();
			gameWindow.getInputsManager().reload();
			gameWindow.getRenderingContext().getFontRenderer().reloadFonts();

			return;
		}

		gameWindow.queueSynchronousTask(new Runnable()
		{
			@Override
			public void run()
			{
				//ModsManager.reload();
				gameContent.reload();
				gameWindow.getInputsManager().reload();
				gameWindow.getRenderingContext().getFontRenderer().reloadFonts();
				
				waitForReload.signal();
			}
		});

		waitForReload.traverse();
	}

	@Override
	public void printChat(String textToPrint)
	{
		if (gameWindow.getLayer() instanceof Ingame)
			((Ingame) gameWindow.getLayer()).chat.insert(textToPrint);
	}

	public void openInventories(Inventory... inventories)
	{
		gameWindow.queueSynchronousTask(new Runnable()
		{
			@Override
			public void run()
			{
				if (gameWindow.getLayer().getRootLayer() instanceof Ingame)
				{
					Ingame gmp = (Ingame) gameWindow.getLayer().getRootLayer();
					gameWindow.setLayer(new InventoryOverlay(gameWindow, gmp, inventories));
					gmp.focus(false);
				}
			}
		});
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
		
		Runnable job = new Runnable() {
			@Override
			public void run()
			{
				//Setup the new world and make a controller for it
				Client.this.world = world;
				clientSideController = new ClientWorldController(Client.this, world);

				//Change the scene
				Ingame ingameScene = new Ingame(gameWindow, world);

				//We want to keep the connection overlay when getting into a server
				if (gameWindow.getLayer() instanceof ConnectionOverlay)
				{
					ConnectionOverlay overlay = (ConnectionOverlay) gameWindow.getLayer();
					//If that happen, we want this connection overlay to forget he was originated from a server browser or whatever
					overlay.setParentScene(ingameScene);
				}
				else
					gameWindow.setLayer(ingameScene);
				
				//Switch scene but keep the overlay
				//ingameScene.changeOverlay(overlay);
				
				//Start only the logic after all that
				world.startLogic();
			}
		};
		
		if(gameWindow.isInstanceMainGLWindow())
			job.run();
		else {
			Fence fence = gameWindow.queueSynchronousTask(job);
			fence.traverse();
		}
		
	}

	@Override
	public void exitToMainMenu()
	{
		gameWindow.queueSynchronousTask(new Runnable()
		{
			@Override
			public void run()
			{
				Layer currentRootLayer = gameWindow.getLayer().getRootLayer();
				if(currentRootLayer != null && currentRootLayer instanceof Ingame) {
					currentRootLayer.destroy();
				}
				
				gameWindow.setLayer(new MainMenuOverlay(gameWindow, new MainMenu(gameWindow)));
				
				if (world != null)
				{
					Client.this.world.destroy();
					Client.this.world = null;
				}
				clientSideController = null;
				
				Client.this.getSoundManager().stopAnySound();
			}
		});
	}

	public void exitToMainMenu(String errorMessage)
	{
		gameWindow.queueSynchronousTask(new Runnable()
		{
			@Override
			public void run()
			{
				gameWindow.setLayer(new MessageBoxOverlay(gameWindow, new MainMenu(gameWindow), errorMessage));
				
				if (world != null)
				{
					Client.this.world.destroy();
					Client.this.world = null;
				}
				clientSideController = null;
				
				Client.this.getSoundManager().stopAnySound();
			}
		});
	}

	@Override
	public void print(String message)
	{
		ChunkStoriesLoggerImplementation.getInstance().info(message);
		printChat(message);
	}

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
		return gameWindow.getInputsManager();
	}

	public GameWindowOpenGL_LWJGL3 getGameWindow()
	{
		return gameWindow;
	}

	@Override
	public ChunkStoriesLogger logger() {
		return this.logger;
	}

	@Override
	public String username() {
		return Client.username;
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
