package io.xol.chunkstories.item;

import io.xol.chunkstories.tools.ChunkStoriesLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ItemsList
{
	public static Item[] items = new Item[65536];
	public static Map<String, Item> dictionary = new HashMap<String, Item>();
	public static int itemTypes = 0;
	public static int lastAllocatedId;
	
	
	public static void reload()
	{
		Arrays.fill(items, null);
		dictionary.clear();
		
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
			
			Item currentItem = null;
			while ((line = reader.readLine()) != null)
			{
				line = line.replace("\t", "");
				if (line.startsWith("#"))
				{
					// It's a comment, ignore.
				}
				else if(line.startsWith("end"))
				{
					if(currentItem == null)
					{
						ChunkStoriesLogger.getInstance().warning("Syntax error in file : "+f+" : ");
						continue;
					}
					//Eventually add the item
					items[currentItem.getID()] = currentItem;
					dictionary.put(currentItem.getInternalName(), currentItem);
				}
				else if(line.startsWith("width"))
				{
					String[] split = line.replaceAll(" ", "").split(":");
					int value = Integer.parseInt(split[1]);
					if(currentItem != null)
						currentItem.setSlotsWidth(value);
				}
				else if(line.startsWith("height"))
				{
					String[] split = line.replaceAll(" ", "").split(":");
					int value = Integer.parseInt(split[1]);
					if(currentItem != null)
						currentItem.setSlotsHeight(value);
				}
				else if(line.startsWith("item"))
				{
					if(line.contains(" "))
					{
						String[] split = line.split(" ");
						int id = Integer.parseInt(split[1]);
						String itemName = split[2];
						String className = split[3];
						
						try
						{
							Class<?> rawClass = Class.forName(className);
							if(rawClass == null)
							{
								ChunkStoriesLogger.getInstance().warning("item "+className+" does not exist in codebase.");
							}
							else if(!(Item.class.isAssignableFrom(rawClass)))
							{
								ChunkStoriesLogger.getInstance().warning("item "+className+" is not extending the Item class.");
							}
							else
							{
								@SuppressWarnings("unchecked")
								Class<? extends Item> itemClass = (Class<? extends Item>)rawClass;
								Class<?>[] types = { Integer.TYPE };
								Constructor<? extends Item> constructor = (Constructor<? extends Item>) itemClass.getConstructor(types);
								//itemClass.getField("internalName").setAccessible(true);
								//itemClass.getField("internalName").set(null, itemName);
								//Field eId = itemClass.getField("allocatedID");
								//System.out.println("Setting "+className+" id to : "+id);
								//eId.setShort(null, id);
								Object[] parameters = { id };
								if(constructor == null)
								{
									System.out.println("item "+className+" does not provide a valid constructor.");
									continue;
								}
								currentItem = constructor.newInstance(parameters);
								currentItem.setInternalName(itemName);
								
								
								//Object[] parameters = { id, name };
								//voxel = (Voxel) constructor.newInstance(parameters);
							}

						}
						catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalArgumentException | IllegalAccessException | InstantiationException | InvocationTargetException e)
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
	
	public static Item get(int id)
	{
		//Quick & dirty sanitization
		id = id & 0x00FFFFFF;
		return items[id];
	}
	
	public static Item getItemByName(String itemName)
	{
		if(dictionary.containsKey(itemName))
			return dictionary.get(itemName);
		return null;
	}
}
