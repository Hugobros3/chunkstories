//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.voxel.material;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.content.Content;
import io.xol.chunkstories.api.voxel.materials.Material;
import io.xol.chunkstories.api.content.Asset;
import io.xol.chunkstories.content.GameContentStore;

public class MaterialsStore implements Content.Materials
{
	private final GameContentStore store;
	
	private static final Logger logger = LoggerFactory.getLogger("content.materials");
	public Logger logger() {
		return logger;
	}
	
	public MaterialsStore(GameContentStore store)
	{
		this.store = store;
	}
	
	Map<String, Material> materials = new HashMap<String, Material>();

	public void reload()
	{
		materials.clear();
		
		Iterator<Asset> i = store.modsManager().getAllAssetsByExtension("materials");
		while(i.hasNext())
		{
			Asset f = i.next();
			readitemsDefinitions(f);
		}
	}

	private void readitemsDefinitions(Asset f)
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
				//We shouldn't come accross end tags by ourselves, this is dealt with in the constructors
				else if (line.startsWith("end"))
				{
					//if (material == null)
					{
						logger().warn("Syntax error in file : " + f + " : ");
						continue;
					}
				}
				else if (line.startsWith("material"))
				{
					if (line.contains(" "))
					{
						String[] split = line.split(" ");
						String materialName = split[1];

						material = new MaterialImplementation(materialName, reader);

						//Eventually add the material
						materials.put(material.getName(), material);
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
	
	public Material getMaterialByName(String name)
	{
		Material material = materials.get(name);
		if(material != null)
			return material;
		
		return getMaterialByName("undefined");
	}

	@Override
	public Iterator<Material> all()
	{
		return materials.values().iterator();
	}

	@Override
	public Content parent()
	{
		return store;
	}
}
