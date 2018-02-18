package io.xol.chunkstories.entity;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.content.Content.EntityDefinitions;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityDefinition;
import io.xol.chunkstories.api.exceptions.content.IllegalEntityDeclarationException;
import io.xol.chunkstories.materials.GenericNamedConfigurable;

public class EntityDefinitionImplementation extends GenericNamedConfigurable implements EntityDefinition {
	
	//final String name;
	//final String className;
	final EntityDefinitionsStore store;
	
	final Constructor<? extends Entity> constructor;

	@SuppressWarnings("unchecked")
	public EntityDefinitionImplementation(EntityDefinitionsStore store, String name, BufferedReader reader) throws IllegalEntityDeclarationException, IOException
	{
		super(name, reader);
		this.store = store;
		
		String className = this.resolveProperty("class", null);
		if(className == null)
			throw new IllegalEntityDeclarationException("'class' property isn't set for entity "+name);
		
		try
		{
			Class<?> entityClass = store.parent().modsManager().getClassByName(className);
			if(entityClass == null)
			{
				throw new IllegalEntityDeclarationException("Entity class " + className + " does not exist in codebase.");
			}
			else if (!(Entity.class.isAssignableFrom(entityClass)))
			{
				throw new IllegalEntityDeclarationException("Entity class " + entityClass + " is not implementing the Entity interface.");
			}
			else
			{
				@SuppressWarnings("rawtypes")
				Class[] types = { EntityDefinition.class, Location.class  };
				
				this.constructor = (Constructor<? extends Entity>) entityClass.getConstructor(types);
				
				if(constructor == null)
				{
					throw new IllegalEntityDeclarationException("Entity "+name+" does not provide a valid constructor.");
				}
			}

		}
		catch (NoSuchMethodException | SecurityException | IllegalArgumentException e)
		{
			e.printStackTrace();
			//e.printStackTrace(store.parent().logger().getPrintWriter());

			throw new IllegalEntityDeclarationException("Entity "+name+" failed to provide an acceptable constructor, exception="+e.getMessage());
		}
	}
	
	@Override
	public Entity create(Location location) {
		Object[] parameters = { this, location };
		try
		{
			return constructor.newInstance(parameters);
		}
		catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
		{
			//This is bad
			store().logger().error("Couldn't instanciate entity "+this+" at " + location);
			store().logger().error("{}",e.getMessage());
			//e.printStackTrace();
			//e.printStackTrace(logger().getPrintWriter());
			return null;
		}
	}

	@Override
	public EntityDefinitions store() {
		return store;
	}
}
