package io.xol.chunkstories.entity;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.xol.chunkstories.content.Mods;
import io.xol.chunkstories.content.mods.Asset;
import io.xol.chunkstories.tools.ChunkStoriesLogger;

public class EntityComponents
{
	static Map<String, Integer> entityComponentsIds = new HashMap<String, Integer>();

	public static void reload()
	{
		entityComponentsIds.clear();
		
		Iterator<Asset> i = Mods.getAllAssetsByExtension("components");
		while(i.hasNext())
		{
			Asset f = i.next();
			ChunkStoriesLogger.getInstance().log("Reading entity components definitions in : " + f);
			readEntityComponentsDefinitions(f);
		}
	}

	private static void readEntityComponentsDefinitions(Asset f)
	{
		if (f == null)
			return;
		try
		{
			BufferedReader reader = new BufferedReader(f.reader());
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
