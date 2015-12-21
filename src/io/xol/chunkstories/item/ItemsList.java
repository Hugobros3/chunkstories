package io.xol.chunkstories.item;

import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.chunkstories.world.World;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ItemsList
{
	static Map<Short, Constructor<? extends Item>> itemsTypes = new HashMap<Short, Constructor<? extends Item>>();
	static Map<String, Short> itemsIds = new HashMap<String, Short>();
	
	public static void reload()
	{
		itemsIds.clear();
		itemsTypes.clear();
		File vanillaFolder = new File("./" + "res/items/");
		for (File f : vanillaFolder.listFiles())
		{
			if (!f.isDirectory() && f.getName().endsWith(".items"))
			{
				ChunkStoriesLogger.getInstance().log("Reading items definitions in : " + f.getAbsolutePath());
				readitemsDefinitions(f);
			}
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
			while ((line = reader.readLine()) != null)
			{
				line = line.replace("\t", "");
				if (line.startsWith("#"))
				{
					// It's a comment, ignore.
				}
				else
				{
					if(line.contains(" "))
					{
						String[] split = line.split(" ");
						short id = Short.parseShort(split[1]);
						String className = split[2];
						
						try
						{
							Class<?> itemClass = Class.forName(className);
							if(itemClass == null)
							{
								System.out.println("item "+className+" does not exist in codebase.");
							}
							else
							{
								@SuppressWarnings("rawtypes")
								Class[] types = {  };
								@SuppressWarnings("unchecked")
								Constructor<? extends Item> constructor = (Constructor<? extends Item>) itemClass.getConstructor(types);
								
								//Field eId = itemClass.getField("allocatedID");
								//System.out.println("Setting "+className+" id to : "+id);
								//eId.setShort(null, id);
								
								if(constructor == null)
								{
									System.out.println("item "+className+" does not provide a valid constructor.");
								}
								else
								{
									itemsTypes.put(id, constructor);
									itemsIds.put(className, id);
								}
								//Object[] parameters = { id, name };
								//voxel = (Voxel) constructor.newInstance(parameters);
							}

						}
						catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalArgumentException e)
						{
							e.printStackTrace();
						}
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

	public static Item newItem(World world, short itemType)
	{
		if(itemsTypes.containsKey(itemType))
		{
			Object[] parameters = {  };
			try
			{
				Item item = itemsTypes.get(itemType).newInstance(parameters);
				return item;
			}
			catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public static short getIdForClass(String className)
	{
		return itemsIds.get(className);
	}

	public Collection<Short> getAllItemIds()
	{
		return itemsIds.values();
	}
}
