package io.xol.chunkstories.entity;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.content.Mods;
import io.xol.chunkstories.content.mods.Asset;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.chunkstories.world.WorldImplementation;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class Entities
{
	static Map<Short, Constructor<? extends Entity>> entitiesTypes = new HashMap<Short, Constructor<? extends Entity>>();
	static Map<String, Short> entitiesIds = new HashMap<String, Short>();
	
	public static void reload()
	{
		entitiesIds.clear();
		entitiesTypes.clear();
		
		Iterator<Asset> i = Mods.getAllAssetsByExtension("entities");
		while(i.hasNext())
		{
			Asset f = i.next();
			ChunkStoriesLogger.getInstance().log("Reading entities definitions in : " + f);
			readEntitiesDefinitions(f);
		}
	}

	private static void readEntitiesDefinitions(Asset f)
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
					if(line.contains(" "))
					{
						String[] split = line.split(" ");
						short id = Short.parseShort(split[0]);
						String className = split[1];
						
						try
						{
							Class<?> entityClass = Mods.getClassByName(className);
							if(entityClass == null)
							{
								System.out.println("Entity class "+className+" does not exist in codebase.");
							}
							else if (!(Entity.class.isAssignableFrom(entityClass)))
							{
								ChunkStoriesLogger.getInstance().warning("Entity class " + entityClass + " is not implementing the Entity interface.");
							}
							else
							{
								@SuppressWarnings("rawtypes")
								Class[] types = { WorldImplementation.class, Double.TYPE, Double.TYPE , Double.TYPE  };
								@SuppressWarnings("unchecked")
								Constructor<? extends Entity> constructor = (Constructor<? extends Entity>) entityClass.getConstructor(types);
								
								if(constructor == null)
								{
									System.out.println("Entity "+className+" does not provide a valid constructor.");
								}
								else
								{
									entitiesTypes.put(id, constructor);
									entitiesIds.put(className, id);
								}
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

	public static Entity newEntity(World world, short entityType)
	{
		if(entitiesTypes.containsKey(entityType))
		{
			Object[] parameters = { world, 0d, 0d, 0d };
			try
			{
				Entity entity = entitiesTypes.get(entityType).newInstance(parameters);
				return entity;
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
		return entitiesIds.get(className);
	}

}
