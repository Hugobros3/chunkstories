//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

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
import io.xol.chunkstories.content.GenericNamedConfigurable;

public class EntityDefinitionImplementation extends GenericNamedConfigurable implements EntityDefinition {
	final EntityDefinitionsStore store;
	final Constructor<? extends Entity> constructor;

	final boolean collidesWithEntities;

	@SuppressWarnings("unchecked")
	public EntityDefinitionImplementation(EntityDefinitionsStore store, String name, BufferedReader reader)
			throws IllegalEntityDeclarationException, IOException {
		super(name, reader);
		this.store = store;

		collidesWithEntities = this.resolveProperty("collidesWithEntities", "false").equals("true");

		String className = this.resolveProperty("class", null);
		if (className == null)
			throw new IllegalEntityDeclarationException("'class' property isn't set for entity " + name);

		try {
			Class<?> entityClass = store.parent().modsManager().getClassByName(className);
			if (entityClass == null) {
				throw new IllegalEntityDeclarationException(
						"Entity class " + className + " does not exist in codebase.");
			} else if (!(Entity.class.isAssignableFrom(entityClass))) {
				throw new IllegalEntityDeclarationException(
						"Entity class " + entityClass + " is not implementing the Entity interface.");
			} else {
				@SuppressWarnings("rawtypes")
				Class[] types = { EntityDefinition.class, Location.class };

				this.constructor = (Constructor<? extends Entity>) entityClass.getConstructor(types);

				if (constructor == null) {
					throw new IllegalEntityDeclarationException(
							"Entity " + name + " does not provide a valid constructor.");
				}
			}

		} catch (NoSuchMethodException | SecurityException | IllegalArgumentException e) {
			e.printStackTrace();
			// e.printStackTrace(store.parent().logger().getPrintWriter());

			throw new IllegalEntityDeclarationException(
					"Entity " + name + " failed to provide an acceptable constructor, exception=" + e.getMessage());
		}
	}

	@Override
	public Entity create(Location location) {
		Object[] parameters = { this, location };
		try {
			Entity entity = constructor.newInstance(parameters);
			entity.afterIntialization();
			return entity;
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			// This is bad
			store().logger().error("Couldn't instanciate entity " + this + " at " + location, e);
			e.printStackTrace();
			// e.printStackTrace(logger().getPrintWriter());
			return null;
		}
	}

	@Override
	public EntityDefinitions store() {
		return store;
	}

	@Override
	public boolean collidesWithEntities() {
		return collidesWithEntities;
	}
}
