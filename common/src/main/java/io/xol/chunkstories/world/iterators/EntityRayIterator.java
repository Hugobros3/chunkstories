package io.xol.chunkstories.world.iterators;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import io.xol.chunkstories.api.entity.Entity;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import io.xol.chunkstories.world.WorldImplementation;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class EntityRayIterator implements Iterator<Entity>
{
	WorldImplementation world;
	Vector3dc initialPosition;
	Vector3dc direction;
	
	List<Entity> sortedEntities = new ArrayList<Entity>();
	Iterator<Entity> lazyFuck;
	
	public EntityRayIterator(WorldImplementation world, Vector3dc initialPosition, Vector3dc direction, double limit)
	{
		this.world = world;
		this.initialPosition = initialPosition;
		this.direction = direction;
		
		Iterator<Entity> iterator = world.getAllLoadedEntities();
		while(iterator.hasNext())
		{
			Entity entity = iterator.next();
			//Distance check
			if(limit == -1 || entity.getLocation().distance(initialPosition) <= limit)
			{
				
				Vector3d toEntity = new Vector3d(entity.getLocation());
				toEntity.sub(initialPosition);
				//Check direction of the line to avoid hitting hitself, backtracking and wrapping arround the world
				if(direction.dot(toEntity) > 0)
				{
					//Line collision checks
					//System.out.println(initialPosition);
					//If it collides with the ray at some point
					if(entity.getTranslatedBoundingBox().lineIntersection(initialPosition, direction) != null)
					{
						//System.out.println(entity+""+initialPosition);
						sortedEntities.add(entity);
						//break;
					}
					
				}
			}
		}
		
		//Sort entities from nearest to furthest to initial position
		sortedEntities.sort(new Comparator<Entity>()
		{
			@Override
			public int compare(Entity a, Entity b)
			{
				double distanceA = a.getLocation().distance(initialPosition);
				double distanceB = b.getLocation().distance(initialPosition);
				
				if(distanceA < distanceB)
					return -1;
				else
					return 1;
				//return distanceA - distanceB;
			}
		});
		
		lazyFuck = sortedEntities.iterator();
	}

	@Override
	public boolean hasNext()
	{
		return lazyFuck.hasNext();
	}

	@Override
	public Entity next()
	{
		return lazyFuck.next();
	}

}
