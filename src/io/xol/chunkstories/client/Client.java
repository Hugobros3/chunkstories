package io.xol.chunkstories.client;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import io.xol.engine.base.XolioWindow;
import io.xol.engine.gui.GuiDrawer;
import io.xol.engine.misc.ConfigFile;
import io.xol.engine.misc.IconLoader;
import io.xol.engine.misc.NativesLoader;
import io.xol.engine.sound.ALSoundManager;
import io.xol.chunkstories.GameData;
import io.xol.chunkstories.GameDirectory;
import io.xol.chunkstories.VersionInfo;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.sound.SoundManager;
import io.xol.chunkstories.client.net.ServerConnection;
import io.xol.chunkstories.entity.Controller;
import io.xol.chunkstories.gui.MainMenu;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.chunkstories.tools.DebugProfiler;
import io.xol.chunkstories.world.World;

public class Client implements Controller
{
	public static ConfigFile clientConfig = new ConfigFile("config/client.cfg");

	public static SoundManager soundManager;
	
	public static boolean offline = false;

	public static XolioWindow windows;
	public static World world;
	public static Entity controller;
	public static ServerConnection connection;

	public static String username = "Unknow";
	public static String session_key = "nopeMLG";

	public static DebugProfiler profiler = new DebugProfiler();

	public static void main(String[] args)
	{
		// FastConfig.define();
		GameDirectory.initClientPath();
		for (String s : args) // Debug arguments
		{
			if (s.equals("-oldgl"))
			{
				FastConfig.openGL3Capable = false;
				FastConfig.doShadows = false;
				System.out.println("Legacy OpenGL mode enabled");
			}
			else if (s.contains("-vd"))
			{
				int vd = Integer.parseInt(s.replace("-vd=", "")) * 2;
				// WorldRenderer.VBO_ARRAY_SIZE = vd;
				FastConfig.viewDistance = vd * 16;
				System.out.println("View distance = " + Integer.parseInt(s.replace("-vd=", "")));
			}
			/*if (s.contains("-cd"))
			{
				int cd = Integer.parseInt(s.replace("-cd=", "")) * 2;
				// WorldRenderer.VBO_ARRAY_SIZE = vd;
				ChunksData.CACHE_SIZE = cd;
				System.out.println("Chunk cache size = " + Integer.parseInt(s.replace("-cd=", "")));
			}*/
			else if (s.contains("-dir"))
			{
				GameDirectory.set(s.replace("-dir=", ""));
			}
			// System.out.println("Argument : "+s);
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
		windows.run();
	}

	public static void onStart()
	{
		IconLoader.load();
		GuiDrawer.initGL();
	}

	public static SoundManager getSoundManager()
	{
		return soundManager;
	}
	
	public static void onClose()
	{
		GuiDrawer.free();
		soundManager.destroy();
		clientConfig.save();
		//ChunkStoriesLogger.getInstance().close();
	}

	public static ConfigFile getConfig()
	{
		return clientConfig;
	}
}
