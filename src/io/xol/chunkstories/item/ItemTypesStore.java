package io.xol.chunkstories.item;

import io.xol.chunkstories.api.Content;
import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.item.ItemType;
import io.xol.chunkstories.api.mods.Asset;
import io.xol.chunkstories.api.mods.ModsManager;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.content.GameContentStore;
import io.xol.chunkstories.tools.ChunkStoriesLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ItemTypesStore implements Content.ItemsTypes
{
	public ItemType[] items = new ItemType[65536];
	Map<Short, Constructor<? extends Item>> itemsTypes = new HashMap<Short, Constructor<? extends Item>>();
	public Map<String, ItemType> dictionary = new HashMap<String, ItemType>();
	public int itemTypes = 0;
	public int lastAllocatedId;

	private final Content content;
	private final ModsManager modsManager;
	
	public ItemTypesStore(GameContentStore gameContentStore)
	{
		this.content = gameContentStore;
		this.modsManager = gameContentStore.modsManager();
		
		reload();
	}
	
	public void reload()
	{
		Arrays.fill(items, null);
		dictionary.clear();
		
		Iterator<Asset> i = modsManager.getAllAssetsByExtension("items");
		while(i.hasNext())
		{
			Asset f = i.next();
			ChunkStoriesLogger.getInstance().log("Reading items definitions in : " + f);
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
							Class<?> rawClass = modsManager.getClassByName(className);
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
								currentItemType = new ItemTypeImpl(this, id);
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
				else
				{
					//Unknown property ? add it to the unknown ones !
					if(line.contains(": "))
					{
						String propertyName = line.split(": ")[0];
						String value = line.split(": ")[1];
						currentItemType.setup(propertyName, value);
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

	public ItemType getItemTypeById(int id)// throws UndefinedItemTypeException
	{
		//Quick & dirty sanitization
		id = id & 0x00FFFFFF;
		return items[id];
	}

	public ItemType getItemTypeByName(String itemName)// throws UndefinedItemTypeException
	{
		if (dictionary.containsKey(itemName))
			return dictionary.get(itemName);
		return null;
	}

	@Override
	public Iterator<ItemType> all()
	{
		return dictionary.values().iterator();
	}

	@Override
	public Content parent()
	{
		return content;
	}
}
