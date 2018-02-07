package io.xol.chunkstories.entity;

import io.xol.chunkstories.api.content.Content;
import io.xol.chunkstories.api.content.Content.EntityTypes;
import io.xol.chunkstories.api.entity.EntityDefinition;
import io.xol.chunkstories.api.exceptions.content.IllegalEntityDeclarationException;
import io.xol.chunkstories.api.content.Asset;
import io.xol.chunkstories.content.GameContentStore;
import io.xol.chunkstories.item.EntityTypeImpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class EntityTypesStore implements EntityTypes
{
	private final Content content;
	
	private Map<Short, EntityDefinition> entityTypesById = new HashMap<Short, EntityDefinition>();
	private Map<String, EntityDefinition> entityTypesByName = new HashMap<String, EntityDefinition>();

	private static final Logger logger = LoggerFactory.getLogger("content.entities");
	public Logger logger() {
		return logger;
	}
	
	public EntityTypesStore(GameContentStore content)
	{
		this.content = content;
	}
	
	public void reload()
	{
		entityTypesById.clear();
		entityTypesByName.clear();
		
		Iterator<Asset> i = content.modsManager().getAllAssetsByExtension("entities");
		while(i.hasNext())
		{
			Asset f = i.next();
			readEntitiesDefinitions(f);
		}
		
		//this.entityComponents.reload();
	}

	private void readEntitiesDefinitions(Asset f)
	{
		if (f == null)
			return;

		logger().debug("Reading entities definitions in : " + f);
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

	/*class EntityTypeLoaded implements EntityDefinition {

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
				logger().log("Couldn't instanciate entity "+this+" in world "+world);
				e.printStackTrace();
				e.printStackTrace(logger().getPrintWriter());
				return null;
			}
		}
		
	}*/

	@Override
	public EntityDefinition getEntityTypeById(short entityId)
	{
		return entityTypesById.get(entityId);
	}

	@Override
	public EntityDefinition getEntityTypeByName(String entityName)
	{
		return entityTypesByName.get(entityName);
	}

	@Deprecated
	public EntityDefinition getEntityTypeByClassname(String className)
	{
		throw new UnsupportedOperationException();
		//return entityTypesByClassname.get(className);
	}

	@Deprecated
	public short getEntityIdByClassname(String className)
	{
		throw new UnsupportedOperationException();
		/*
		EntityDefinition type = entityTypesByClassname.get(className);
		if(type == null)
			return -1;
		return type.getId();*/
	}

	@Override
	public Iterator<EntityDefinition> all()
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
