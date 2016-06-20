package io.xol.chunkstories.entity;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.chunkstories.world.WorldImplementation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class EntitiesList
{
	static Map<Short, Constructor<? extends Entity>> entitiesTypes = new HashMap<Short, Constructor<? extends Entity>>();
	static Map<String, Short> entitiesIds = new HashMap<String, Short>();
	
	public static void reload()
	{
		entitiesIds.clear();
		entitiesTypes.clear();
		File vanillaFolder = new File("./" + "res/entities/");
		for (File f : vanillaFolder.listFiles())
		{
			if (!f.isDirectory() && f.getName().endsWith(".entities"))
			{
				ChunkStoriesLogger.getInstance().log("Reading entities definitions in : " + f.getAbsolutePath());
				readEntitiesDefinitions(f);
			}
		}
	}

	private static void readEntitiesDefinitions(File f)
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
						short id = Short.parseShort(split[0]);
						String className = split[1];
						
						try
						{
							Class<?> entityClass = Class.forName(className);
							if(entityClass == null)
							{
								System.out.println("Entity "+className+" does not exist in codebase.");
							}
							else
							{
								@SuppressWarnings("rawtypes")
								Class[] types = { WorldImplementation.class, Double.TYPE, Double.TYPE , Double.TYPE  };
								@SuppressWarnings("unchecked")
								Constructor<? extends Entity> constructor = (Constructor<? extends Entity>) entityClass.getConstructor(types);
								
								//Field eId = entityClass.getField("allocatedID");
								//System.out.println("Setting "+className+" id to : "+id);
								//eId.setShort(null, id);
								
								if(constructor == null)
								{
									System.out.println("Entity "+className+" does not provide a valid constructor.");
								}
								else
								{
									entitiesTypes.put(id, constructor);
									entitiesIds.put(className, id);
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
