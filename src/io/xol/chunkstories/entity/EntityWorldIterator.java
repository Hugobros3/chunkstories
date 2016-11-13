package io.xol.chunkstories.entity;

import java.util.Iterator;
import java.util.Set;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.utils.IterableIterator;

public class EntityWorldIterator implements IterableIterator<Entity>
{
	Iterator<Entity> ie;
	Entity currentEntity;

	public EntityWorldIterator(Iterator<Entity> ie)
	{
		this.ie = ie;
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
		//System.out.println(currentEntity);
		return currentEntity;
	}

	@Override
	public void remove()
	{
		//Remove it from the world set
		ie.remove();
	}
}
