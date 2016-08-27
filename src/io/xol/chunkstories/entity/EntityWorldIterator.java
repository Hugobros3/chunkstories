package io.xol.chunkstories.entity;

import java.util.Iterator;
import java.util.Set;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.utils.IterableIterator;

public class EntityWorldIterator implements IterableIterator<Entity>
{
	Set<Entity> entities;
	Iterator<Entity> ie;
	Entity currentEntity;

	public EntityWorldIterator(Set<Entity> entities)
	{
		this.entities = entities;
		ie = entities.iterator();
	}

	@Override
	public boolean hasNext()
	{
		return ie.hasNext();
	}

	@Override
	public Entity next()
	{
		currentEntity = ie.next();
		return currentEntity;
	}

	@Override
	public void remove()
	{
		//Remove it from the world set
		ie.remove();
	}
}
