package io.xol.chunkstories.entity;

import java.util.Iterator;
import java.util.concurrent.BlockingQueue;

public class EntityIterator implements Iterator<Entity>
{

	BlockingQueue<Entity> entities;
	Iterator<Entity> ie;
	Entity currentEntity;

	public EntityIterator(BlockingQueue<Entity> entities)
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
		if(currentEntity != null)
		{
			System.out.println("Iterator removal !");
			currentEntity.delete();
		}
		ie.remove();
	}
}
