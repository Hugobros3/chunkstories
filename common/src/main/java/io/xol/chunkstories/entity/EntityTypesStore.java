package io.xol.chunkstories.entity;

import io.xol.chunkstories.api.content.Content;
import io.xol.chunkstories.api.content.Content.EntityTypes;
import io.xol.chunkstories.api.entity.EntityType;
import io.xol.chunkstories.api.exceptions.content.IllegalEntityDeclarationException;
import io.xol.chunkstories.api.content.Asset;
import io.xol.chunkstories.content.GameContentStore;
import io.xol.chunkstories.item.EntityTypeImpl;
import io.xol.chunkstories.tools.ChunkStoriesLoggerImplementation;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class EntityTypesStore implements EntityTypes
{
	//private final GameContext context;
	private final Content content;
	//private final EntityComponentsStore entityComponents;
	
	private Map<Short, EntityType> entityTypesById = new HashMap<Short, EntityType>();
	private Map<String, EntityType> entityTypesByName = new HashMap<String, EntityType>();
	//private Map<String, EntityType> entityTypesByClassname = new HashMap<String, EntityType>();

	public EntityTypesStore(GameContentStore content)
	{
		this.content = content;
		//this.context = content.getContext();
		
		//this.entityComponents = new EntityComponentsStore(context, this);
		
		//this.reload();
	}
	
	public void reload()
	{
		entityTypesById.clear();
		entityTypesByName.clear();
		//entityTypesByClassname.clear();
		
		Iterator<Asset> i = content.modsManager().getAllAssetsByExtension("entities");
		while(i.hasNext())
		{
			Asset f = i.next();
			ChunkStoriesLoggerImplementation.getInstance().log("Reading entities definitions in : " + f);
			readEntitiesDefinitions(f);
		}
		
		//this.entityComponents.reload();
	}

	private void readEntitiesDefinitions(Asset f)
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
					if(line.startsWith("entity "))
					{
						String[] split = line.split(" ");
						String name = split[1];
						short id = Short.parseShort(split[2]);
						
						try
						{
							EntityTypeImpl entityType = new EntityTypeImpl(this, name, id, reader);

							this.entityTypesById.put(entityType.getId(), entityType);
							this.entityTypesByName.put(entityType.getName(), entityType);
						}
						catch (IllegalEntityDeclarationException e)
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

	/*class EntityTypeLoaded implements EntityType {

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
			Object[] parameters = { this, world, 0d, 0d, 0d };
			try
			{
				return constructor.newInstance(parameters);
			}
			catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
			{
				//This is bad
				ChunkStoriesLoggerImplementation.getInstance().log("Couldn't instanciate entity "+this+" in world "+world);
				e.printStackTrace();
				e.printStackTrace(ChunkStoriesLoggerImplementation.getInstance().getPrintWriter());
				return null;
			}
		}
		
	}*/

	@Override
	public EntityType getEntityTypeById(short entityId)
	{
		return entityTypesById.get(entityId);
	}

	@Override
	public EntityType getEntityTypeByName(String entityName)
	{
		return entityTypesByName.get(entityName);
	}

	@Deprecated
	public EntityType getEntityTypeByClassname(String className)
	{
		throw new UnsupportedOperationException();
		//return entityTypesByClassname.get(className);
	}

	@Deprecated
	public short getEntityIdByClassname(String className)
	{
		throw new UnsupportedOperationException();
		/*
		EntityType type = entityTypesByClassname.get(className);
		if(type == null)
			return -1;
		return type.getId();*/
	}

	@Override
	public Iterator<EntityType> all()
	{
		return this.entityTypesById.values().iterator();
	}

	@Override
	public Content parent()
	{
		return content;
	}

	/*@Override
	public EntityComponentsStore components()
	{
		return entityComponents;
	}*/
}
