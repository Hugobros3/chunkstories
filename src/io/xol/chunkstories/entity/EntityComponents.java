package io.xol.chunkstories.entity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.xol.chunkstories.tools.ChunkStoriesLogger;

public class EntityComponents
{
	static Map<String, Integer> entityComponentsIds = new HashMap<String, Integer>();

	public static void reload()
	{
		entityComponentsIds.clear();
		File vanillaFolder = new File("./" + "res/entities/");
		for (File f : vanillaFolder.listFiles())
		{
			if (!f.isDirectory() && f.getName().endsWith(".components"))
			{
				ChunkStoriesLogger.getInstance().log("Reading entity components definitions in : " + f.getAbsolutePath());
				readEntityComponentsDefinitions(f);
			}
		}
	}

	private static void readEntityComponentsDefinitions(File f)
	{
		if (!f.exists())
			return;
		try
		{
			FileReader fileReader = new FileReader(f);
			BufferedReader reader = new BufferedReader(fileReader);
			String line = "";
			while ((line = reader.readLine()) != null)
			{
				line = line.replace("\t", "");
				if (line.startsWith("#"))
				{
					// It's a comment, ignore.
				}
				else
				{
					if (line.contains(" "))
					{
						String[] split = line.split(" ");
						int id = Short.parseShort(split[0]);
						String className = split[1];

						entityComponentsIds.put(className, id);
						
					}
				}
			}
			reader.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static int getIdForClass(String className)
	{
		return entityComponentsIds.get(className);
	}

}
