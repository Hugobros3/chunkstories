package io.xol.chunkstories.materials;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.xol.chunkstories.api.material.Material;
import io.xol.chunkstories.content.Mods;
import io.xol.chunkstories.content.mods.Asset;
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
		
		Iterator<Asset> i = Mods.getAllAssetsByExtension("materials");
		while(i.hasNext())
		{
			Asset f = i.next();
			readitemsDefinitions(f);
		}
	}

	private static void readitemsDefinitions(Asset f)
	{
		if (f == null)
			return;
		try
		{
			BufferedReader reader = new BufferedReader(f.reader());
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
