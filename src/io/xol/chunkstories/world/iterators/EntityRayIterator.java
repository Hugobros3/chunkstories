package io.xol.chunkstories.world.iterators;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.physics.CollisionBox;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.engine.math.lalgb.Vector3d;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class EntityRayIterator implements Iterator<Entity>
{
	WorldImplementation world;
	Vector3d initialPosition;
	Vector3d direction;
	
	List<Entity> sortedEntities = new ArrayList<Entity>();
	Iterator<Entity> lazyFuck;
	
	public EntityRayIterator(WorldImplementation world, Vector3d initialPosition, Vector3d direction, double limit)
	{
		this.world = world;
		this.initialPosition = initialPosition;
		this.direction = direction;
		
		Iterator<Entity> iterator = world.getAllLoadedEntities();
		while(iterator.hasNext())
		{
			Entity entity = iterator.next();
			//Distance check
			if(limit == -1 || entity.getLocation().distanceTo(initialPosition) <= limit)
			{
				
				Vector3d toEntity = new Vector3d(entity.getLocation());
				toEntity.sub(initialPosition);
				//Check direction of the line to avoid hitting hitself, backtracking and wrapping arround the world
				if(direction.dot(toEntity) > 0)
				{
					//Line collision checks
					for(CollisionBox box : entity.getTranslatedCollisionBoxes())
					{
						//If it collides with the ray at some point
						if(box.collidesWith(initialPosition, direction) != null)
						{
							sortedEntities.add(entity);
							break;
						}
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
				double distanceA = a.getLocation().distanceTo(initialPosition);
				double distanceB = b.getLocation().distanceTo(initialPosition);
				
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
