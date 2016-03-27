package io.xol.chunkstories.content;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import java.io.File;

public class GameDirectory
{
	public static File chunkStoriesFolder = new File(".");
	
	public static String getGameFolderPath()
	{
		if (chunkStoriesFolder == null)
			return ".";
		return chunkStoriesFolder.getAbsolutePath();
	}

	public static void initClientPath()
	{
		String appDataFolder = System.getProperty("user.dir");
		if(System.getProperty("os.name").toLowerCase().startsWith("win"))
			appDataFolder = System.getenv("APPDATA");
		else if(System.getProperty("os.name").toLowerCase().startsWith("lin"))
			appDataFolder = System.getProperty("user.home");
		else if(System.getProperty("os.name").toLowerCase().startsWith("mac"))
			appDataFolder = System.getProperty("user.home");
		chunkStoriesFolder = new File(appDataFolder + "/.chunkstories");
	}

	public static void set(String string)
	{
		chunkStoriesFolder = new File(string);
		System.out.println("Game dir = "+ chunkStoriesFolder.getAbsolutePath());
	}

	public static void check()
	{
		if (!chunkStoriesFolder.exists())
		{
			boolean success = chunkStoriesFolder.mkdir();
			if (success)
				System.out
						.println("Successfully created .chunkstories folder.");
			else
			{
				System.out
						.println("Couldn't access or create .chunkstories folder. Exiting.");
				Runtime.getRuntime().exit(0);
			}
		}
		System.out.println("Using " + chunkStoriesFolder.getAbsolutePath() + " as game folder.");
	}
}
