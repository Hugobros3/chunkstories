package io.xol.chunkstories.client;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;

import io.xol.engine.base.XolioWindow;
import io.xol.engine.gui.GuiDrawer;
import io.xol.engine.misc.ConfigFile;
import io.xol.engine.misc.IconLoader;
import io.xol.engine.misc.NativesLoader;
import io.xol.engine.sound.ALSoundManager;
import io.xol.chunkstories.VersionInfo;
import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.entity.ClientController;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.input.KeyBind;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.sound.SoundManager;
import io.xol.chunkstories.client.net.ClientToServerConnection;
import io.xol.chunkstories.content.GameData;
import io.xol.chunkstories.content.GameDirectory;
import io.xol.chunkstories.content.PluginsManager;
import io.xol.chunkstories.gui.GameplayScene;
import io.xol.chunkstories.gui.MainMenu;
import io.xol.chunkstories.input.KeyBinds;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.chunkstories.tools.DebugProfiler;
import io.xol.chunkstories.world.WorldImplementation;

public class Client implements ClientController, ClientInterface
{
	public static ConfigFile clientConfig = new ConfigFile("./config/client.cfg");

	public static SoundManager soundManager;
	public static boolean offline = false;

	public static ClientToServerConnection connection;
	public static XolioWindow windows;
	public static WorldImplementation world;

	public static String username = "Unknow";
	public static String session_key = "nopeMLG";

	public static DebugProfiler profiler = new DebugProfiler();

	public static Entity controlledEntity;
	public static Client clientController;

	public static PluginsManager pluginsManager;

	public static void main(String[] args)
	{
		clientController = new Client();
		GameDirectory.initClientPath();
		for (String s : args) // Debug arguments
		{
			if (s.equals("-oldgl"))
			{
				FastConfig.openGL3Capable = false;
				FastConfig.doShadows = false;
				System.out.println("Legacy OpenGL mode enabled");
			}
			else if (s.equals("-forceobsolete"))
			{
				FastConfig.ignoreObsoleteHardware = false;
				System.out.println("Legacy OpenGL mode enabled");
			}
			else if (s.contains("-vd"))
			{
				int vd = Integer.parseInt(s.replace("-vd=", "")) * 2;
				FastConfig.viewDistance = vd * 16;
				System.out.println("View distance = " + Integer.parseInt(s.replace("-vd=", "")));
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
				System.out.println("Comandline arguments : \n" + "-oldgl Disables OpenGL 3.0+ stuff\n" + "-forceobsolete Forces the game to run even if requirements aren't met\n"
						+ "-mods=xxx,yyy | -mods=* Tells the game to start with those mods enabled\n" + "-dir=whatever Tells the game not to look for .chunkstories at it's normal location and instead use the argument");
			}
		}
		// Check for folder
		GameDirectory.check();
		// Start logs
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("YYYY.MM.dd HH.mm.ss");
		String time = sdf.format(cal.getTime());
		ChunkStoriesLogger.init(new ChunkStoriesLogger(ChunkStoriesLogger.LogLevel.ALL, ChunkStoriesLogger.LogLevel.ALL, new File(GameDirectory.getGameFolderPath() + "/logs/" + time + ".log")));
		// Load natives
		FastConfig.define();
		NativesLoader.load();
		// Load last gamemode
		GameData.reload();
		soundManager = new ALSoundManager();
		// Gl init
		windows = new XolioWindow("Chunk Stories " + VersionInfo.version, -1, -1);
		windows.createContext();
		windows.changeScene(new MainMenu(windows, true));
		KeyBinds.loadKeyBinds();
		pluginsManager = new PluginsManager(clientController);
		windows.run();
	}

	public static void onStart()
	{
		IconLoader.load();
		GuiDrawer.initGL();
	}

	@Override
	public SoundManager getSoundManager()
	{
		return soundManager;
	}

	public static void onClose()
	{
		GuiDrawer.free();
		soundManager.destroy();
		clientConfig.save();
	}

	public static ConfigFile getConfig()
	{
		return clientConfig;
	}

	public static Client getInstance()
	{
		return clientController;
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
		GameData.reloadClientContent();
	}

	@Override
	public KeyBind getKeyBind(String bindName)
	{
		return KeyBinds.getKeyBind(bindName);
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

	@Override
	public long getUUID()
	{
		return Client.username.hashCode();
	}

	@Override
	public Iterator<Entity> getSubscribedToList()
	{
		return null;
	}

	@Override
	public boolean subscribe(Entity entity)
	{
		return false;
	}

	@Override
	public boolean unsubscribe(Entity entity)
	{
		return false;
	}

	@Override
	public void unsubscribeAll()
	{
		
	}

	@Override
	public void pushPacket(Packet packet)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isSubscribedTo(Entity entity)
	{
		if(entity == controlledEntity)
			return true;
		return false;
	}
}
