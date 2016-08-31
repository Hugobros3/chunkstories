package io.xol.chunkstories.materials;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.xol.chunkstories.api.material.Material;
import io.xol.chunkstories.content.GameContent;
import io.xol.chunkstories.tools.ChunkStoriesLogger;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class Materials
{
	static Map<String, MaterialImplementation> materials = new HashMap<String, MaterialImplementation>();

	public static void reload()
	{
		materials.clear();
		
		Iterator<File> i = GameContent.getAllFilesByExtension("materials");
		while(i.hasNext())
		{
			File f = i.next();
			readitemsDefinitions(f);
		}
	}

	private static void readitemsDefinitions(File f)
	{
		if (!f.exists())
			return;
		try
		{
			FileReader fileReader = new FileReader(f);
			BufferedReader reader = new BufferedReader(fileReader);
			String line = "";

			MaterialImplementation material = null;
			while ((line = reader.readLine()) != null)
			{
				line = line.replace("\t", "");
				if (line.startsWith("#"))
				{
					// It's a comment, ignore.
				}
				else if (line.startsWith("end"))
				{
					if (material == null)
					{
						ChunkStoriesLogger.getInstance().warning("Syntax error in file : " + f + " : ");
						continue;
					}
					//Eventually add the material
					materials.put(material.getName(), material);
				}
				else if (line.startsWith("material"))
				{
					if (line.contains(" "))
					{
						String[] split = line.split(" ");
						String materialName = split[1];

						material = new MaterialImplementation(materialName);
					}
				}
				else if(line.contains(":"))
				{
					if (material == null)
					{
						ChunkStoriesLogger.getInstance().warning("Syntax error in file : " + f + " : ");
						continue;
					}
					String[] s = line.split(": ");
					material.setProperty(s[0], s[1]);
				}
			}
			reader.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public static Material getMaterial(String name)
	{
		Material material = materials.get(name);
		if(material != null)
			return material;
		
		return getMaterial("undefined");
	}
}
