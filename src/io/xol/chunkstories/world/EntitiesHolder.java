package io.xol.chunkstories.world;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.xol.chunkstories.api.entity.Entity;

public class EntitiesHolder implements Iterable<Entity>
{
	Map<Long, Entity> backing = new ConcurrentHashMap<Long, Entity>();
	ConcurrentLinkedQueue<Entity> backingIterative = new ConcurrentLinkedQueue<Entity>();
	//LinkedList<Entity> backing = new LinkedList<Entity>();

	//Elements are sorted in increasing hashcode order
	public void insertEntity(Entity entity)
	{
		/*//For security reasons we don't use the hashCode() method
		int hashCode = hashCode(entity.getUUID());
		//System.out.println("adding entity " + entity + " with hascde" + hashCode);

		int position = binarySearch(hashCode);
		backing.add(position, entity);*/
		
		//If the add went smooth.
		if(backing.put(entity.getUUID(), entity) == null)
			backingIterative.add(entity);
	}

	public boolean removeEntity(Entity entity)
	{
		/*long uuid = entityToRemove.getUUID();
		int hashCode = hashCode(uuid);

		//Do a binary lookup to find the first point where hash is
		int position = binarySearch(hashCode);
		while (1 < 2)
		{
			Entity entity = backing.get(position);
			if (entity.getUUID() == uuid)
			{
				System.out.println("Entity " + entityToRemove + " removed correctly.");
				backing.remove(position);
				return true;
			}

			//We went too far and didn't found it
			if (hashCode(entity.getUUID()) != hashCode)
				break;

			position++;
		}*/

		if(backing.remove(entity.getUUID()) != null)
			backingIterative.remove(entity);
		else
			System.out.println("Warning, (rmv) entity " + entity + " not found in entities list.");
		return false;
	}

	public Entity getEntityByUUID(long uuid)
	{
		return backing.get(uuid);
		
		/*int hashCode = hashCode(uuid);

		//Do a binary lookup to find the first point where hash is
		int position = binarySearch(hashCode);
		while (position < backing.size())
		{
			Entity entity = backing.get(position);
			if (entity.getUUID() == uuid)
				return entity;

			//We went too far and didn't found it
			if (hashCode(entity.getUUID()) != hashCode)
				break;

			position++;
		}

		System.out.println("Warning, entity by uuid" + uuid + " not found in entities list.");
		return null;*/
	}

	/*private int binarySearch(int l)
	{
		int lo = 0;
		int hi = backing.size() - 1;

		if (hi < 0)
			return 0;

		int pos = hi / 2;

		while (pos < backing.size() && hi >= lo)
		{
			int f = hashCode(backing.get(pos).getUUID());
			if (f == l)
				break;
			else if (f > l)
			{
				//We went too far !
				hi = pos - 1;
			}
			else
			{
				//We are too low
				lo = pos + 1;
			}

			//System.out.println(lo+":"+pos+":"+hi);

			pos = lo + (hi - lo) / 2;
		}

		//Backs up as much as it can
		while (pos > 0)
		{
			if (hashCode(backing.get(pos - 1).getUUID()) == l)
				pos--;
			else
				break;
		}

		return pos;
	}

	private int hashCode(long uuid)
	{
		return (int) (uuid & 0xFFFFFFFF);
	}*/

	private static final long serialVersionUID = -6957124575819483540L;

	@Override
	public Iterator<Entity> iterator()
	{
		return backingIterative.iterator();
	}
}
