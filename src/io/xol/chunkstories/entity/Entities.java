package io.xol.chunkstories.entity;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityType;
import io.xol.chunkstories.api.mods.Asset;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.content.DefaultModsManager;
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
	//static Map<Short, Constructor<? extends Entity>> entitiesTypes = new HashMap<Short, Constructor<? extends Entity>>();
	//static Map<String, Short> entitiesIds = new HashMap<String, Short>();
	
	static Map<Short, EntityType> entityTypesById = new HashMap<Short, EntityType>();
	static Map<String, EntityType> entityTypesByName = new HashMap<String, EntityType>();
	static Map<String, EntityType> entityTypesByClassname = new HashMap<String, EntityType>();
	
	public static void reload()
	{
		//entitiesIds.clear();
		//entitiesTypes.clear();
		entityTypesById.clear();
		entityTypesByName.clear();
		entityTypesByClassname.clear();
		
		Iterator<Asset> i = DefaultModsManager.getAllAssetsByExtension("entities");
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
						
						String entityTypeName = className.substring(className.lastIndexOf("."));
						if(split.length >= 3)
							entityTypeName = split[2];
							
						try
						{
							Class<?> entityClass = DefaultModsManager.getClassByName(className);
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
									EntityTypeLoaded addMe = new EntityTypeLoaded(entityTypeName, className, constructor, id);

									entityTypesById.put(id, addMe);
									entityTypesByName.put(entityTypeName, addMe);
									entityTypesByClassname.put(className, addMe);
									//entitiesTypes.put(id, constructor);
									//entitiesIds.put(className, id);
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
	
	public static EntityType getEntityTypeById(short entityId)
	{
		return entityTypesById.get(entityId);
	}
	
	public static short getEntityIdByClassname(String className)
	{
		EntityType type = entityTypesByClassname.get(className);
		if(type == null)
			return -1;
		return type.getId();
	}

	static class EntityTypeLoaded implements EntityType {

		public EntityTypeLoaded(String name, String className, Constructor<? extends Entity> constructor, short id)
		{
			super();
			this.name = name;
			this.className = className;
			this.constructor = constructor;
			this.id = id;
		}

		@Override
		public String toString()
		{
			return "EntityDefined [name=" + name + ", className=" + className + ", constructor=" + constructor + ", id=" + id + "]";
		}

		final String name;
		final String className;
		final Constructor<? extends Entity> constructor;
		final short id;
		
		@Override
		public String getName()
		{
			return name;
		}

		@Override
		public short getId()
		{
			return id;
		}

		@Override
		public Entity create(World world)
		{
			Object[] parameters = { world, 0d, 0d, 0d };
			try
			{
				return constructor.newInstance(parameters);
			}
			catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
			{
				//This is bad
				ChunkStoriesLogger.getInstance().log("Couldn't instanciate entity "+this+" in world "+world);
				e.printStackTrace(ChunkStoriesLogger.getInstance().getPrintWriter());
				return null;
			}
		}
		
	}
}
