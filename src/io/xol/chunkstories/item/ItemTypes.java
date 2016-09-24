package io.xol.chunkstories.item;

import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.item.ItemType;
import io.xol.chunkstories.content.GameContent;
import io.xol.chunkstories.tools.ChunkStoriesLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ItemTypes
{
	public static ItemType[] items = new ItemType[65536];
	static Map<Short, Constructor<? extends Item>> itemsTypes = new HashMap<Short, Constructor<? extends Item>>();
	public static Map<String, ItemType> dictionary = new HashMap<String, ItemType>();
	public static int itemTypes = 0;
	public static int lastAllocatedId;

	public static void reload()
	{
		Arrays.fill(items, null);
		dictionary.clear();
		
		Iterator<File> i = GameContent.getAllFilesByExtension("items");
		while(i.hasNext())
		{
			File f = i.next();
			ChunkStoriesLogger.getInstance().log("Reading items definitions in : " + f.getAbsolutePath());
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

			ItemTypeImpl currentItemType = null;
			while ((line = reader.readLine()) != null)
			{
				line = line.replace("\t", "");
				if (line.startsWith("#"))
				{
					// It's a comment, ignore.
				}
				else if (line.startsWith("end"))
				{
					if (currentItemType == null)
					{
						ChunkStoriesLogger.getInstance().warning("Syntax error in file : " + f + " : ");
						continue;
					}
					
					//Eventually add the item
					items[currentItemType.getID()] = currentItemType;
					dictionary.put(currentItemType.getInternalName(), currentItemType);
				}
				else if (line.startsWith("maxStackSize"))
				{
					String[] split = line.replaceAll(" ", "").split(":");
					int value = Integer.parseInt(split[1]);
					if (currentItemType != null)
						currentItemType.setMaxStackSize(value);
				}
				else if (line.startsWith("width"))
				{
					String[] split = line.replaceAll(" ", "").split(":");
					int value = Integer.parseInt(split[1]);
					if (currentItemType != null)
						currentItemType.setSlotsWidth(value);
				}
				else if (line.startsWith("height"))
				{
					String[] split = line.replaceAll(" ", "").split(":");
					int value = Integer.parseInt(split[1]);
					if (currentItemType != null)
						currentItemType.setSlotsHeight(value);
				}
				else if (line.startsWith("item"))
				{
					if (line.contains(" "))
					{
						String[] split = line.split(" ");
						int id = Integer.parseInt(split[1]);
						String itemName = split[2];
						String className = "io.xol.chunkstories.api.item.Item";
						
						if(split.length > 3)
							className = split[3];
						try
						{
							Class<?> rawClass = GameContent.getClassByName(className);
							if (rawClass == null)
							{
								ChunkStoriesLogger.getInstance().warning("Item class " + className + " does not exist in codebase.");
							}
							else if (!(Item.class.isAssignableFrom(rawClass)))
							{
								ChunkStoriesLogger.getInstance().warning("Item class " + className + " is not extending the Item class.");
							}
							else
							{
								@SuppressWarnings("unchecked")
								Class<? extends Item> itemClass = (Class<? extends Item>) rawClass;
								Class<?>[] types = { ItemType.class };
								Constructor<? extends Item> constructor = itemClass.getConstructor(types);
								
								if (constructor == null)
								{
									System.out.println("item " + className + " does not provide a valid constructor.");
									continue;
								}
								currentItemType = new ItemTypeImpl(id);
								currentItemType.setInternalName(itemName);
								currentItemType.setConstructor(constructor);
							}

						}
						catch (NoSuchMethodException | SecurityException | IllegalArgumentException e)
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

	public static ItemType getItemTypeById(int id)
	{
		//Quick & dirty sanitization
		id = id & 0x00FFFFFF;
		return items[id];
	}

	public static ItemType getItemTypeByName(String itemName)
	{
		if (dictionary.containsKey(itemName))
			return dictionary.get(itemName);
		return null;
	}
}
