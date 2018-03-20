//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.joml.Vector3d;
import org.joml.Vector3dc;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.World.NearEntitiesIterator;
import io.xol.chunkstories.api.world.region.Region;

public class WorldEntitiesHolder implements Iterable<Entity>
{
	Map<Long, Entity> backing = new ConcurrentHashMap<Long, Entity>();
	ConcurrentLinkedQueue<Entity> backingIterative = new ConcurrentLinkedQueue<Entity>();

	final World world;
	
	public WorldEntitiesHolder(World world) {
		this.world = world;
	}
	
	//Elements are sorted in increasing hashcode order
	public void insertEntity(Entity entity)
	{
		if(backing.put(entity.getUUID(), entity) == null)
			backingIterative.add(entity);
	}

	public boolean removeEntity(Entity entity)
	{
		if(backing.remove(entity.getUUID()) != null)
			backingIterative.remove(entity);
		else
			System.out.println("Warning, EntitiesHolders was asked to remove entity " + entity + " not found in entities list.");
		
		return false;
	}

	public Entity getEntityByUUID(long uuid)
	{
		return backing.get(uuid);
	}
	
	public NearEntitiesIterator getEntitiesInBox(Vector3dc center, Vector3dc boxSize) {
		
		return new NearEntitiesIterator() {
		
		int centerVoxel_x = (int)(double)center.x();
		int centerVoxel_y = (int)(double)center.y();
		int centerVoxel_z = (int)(double)center.z();
		
		int box_ceil_x = (int)Math.ceil((double) boxSize.x());
		int box_ceil_y = (int)Math.ceil((double) boxSize.y());
		int box_ceil_z = (int)Math.ceil((double) boxSize.z());
		
		int box_start_x = sanitizeHorizontalCoordinate(centerVoxel_x - box_ceil_x);
		int box_start_y = sanitizeVerticalCoordinate(centerVoxel_y - box_ceil_y);
		int box_start_z = sanitizeHorizontalCoordinate(centerVoxel_z - box_ceil_z);
		
		int box_end_x = sanitizeHorizontalCoordinate(centerVoxel_x + box_ceil_x);
		int box_end_y = sanitizeVerticalCoordinate(centerVoxel_y + box_ceil_y);
		int box_end_z = sanitizeHorizontalCoordinate(centerVoxel_z + box_ceil_z);
		
		//We currently sort this out by regions, chunks would be more appropriate ?
		int region_start_x = box_start_x / 256;
		int region_start_y = box_start_y / 256;
		int region_start_z = box_start_z / 256;

		int region_end_x = box_end_x / 256;
		int region_end_y = box_end_y / 256;
		int region_end_z = box_end_z / 256;
		
		int region_x = region_start_x;
		int region_y = region_start_y;
		int region_z = region_start_z;
		
		Region currentRegion = world.getRegion(region_x, region_y, region_z);
		Iterator<Entity> currentRegionIterator = currentRegion == null ? null : currentRegion.getEntitiesWithinRegion();
		Entity next = null;
		double distance = 0D;
		
		private void seekNextEntity() {
			next = null;
			while(true) {
				//Break the loop if we find an entity in the region
				if(seekNextEntityWithinRegion())
					break;
				else
				{
					//Seek a suitable region if we failed to find anything above
					if(seekNextRegion())
						continue;
					//Break the loop if we are out of regions to check
					else
						break;
				}
			}
		}
		
		private boolean seekNextEntityWithinRegion() {
			
			if(currentRegionIterator == null)
				return false;
			while(currentRegionIterator.hasNext())
			{
				Entity entity = currentRegionIterator.next();
				//Check if it's inside the box for realz
				
				Location loc = entity.getLocation();
				
				int locx = (int)(double)loc.x();
				//Normal case, check if it's in the bounds, wrap-arround case, check if it's outside
				if((box_start_x > box_end_x) == (locx >= box_start_x && locx <= box_end_x))
					continue;
				
				int locy = (int)(double)loc.y();
				//Normal case, check if it's in the bounds, wrap-arround case, check if it's outside
				if((box_start_y > box_end_y) == (locy >= box_start_y && locy <= box_end_y))
					continue;
					
				int locz = (int)(double)loc.z();
				//Normal case, check if it's in the bounds, wrap-arround case, check if it's outside
				if((box_start_z > box_end_z) == (locz >= box_start_z && locz <= box_end_z))
					continue;
				
				//if(Math.abs(check.getX()) <= boxSize.getX() && Math.abs(check.getY()) <= boxSize.getY() && Math.abs(check.getZ()) <= boxSize.getZ())
				{
					//Found a good one
					this.next = entity;
					
					Vector3d check = new Vector3d(loc);
					check.sub(center);
					this.distance = check.length();
					return true;
				}
			}
			
			//We found nothing :(
			currentRegionIterator = null;
			return false;
		}
		
		private boolean seekNextRegion() {
			currentRegion = null;
			while(true) {
				//Found one !
				if(currentRegion != null)
				{
					currentRegionIterator = currentRegion.getEntitiesWithinRegion();
					return true;
				}
				
				region_x++;
				//Wrap arround in X dimension to Y
				if(region_x > region_end_x)
				{
					region_x = 0;
					region_y++;
				}
				//Then Y to Z
				if(region_y > region_end_y)
				{
					region_y = 0;
					region_z++;
				}
				//We are done here
				if(region_z > region_end_z)
					return false;
				
				currentRegion = world.getRegion(region_x, region_y, region_z);
			}
		}
		
		@Override
		public boolean hasNext()
		{
			if(next == null)
				seekNextEntity();
			return next != null;
		}
		@Override
		public Entity next()
		{
			Entity entity = next;
			seekNextEntity();
			return entity;
		}
		@Override
		public double distance()
		{
			return distance;
		}
		
		};
	}
	
	private int sanitizeHorizontalCoordinate(int coordinate)
	{
		coordinate = coordinate % (world.getSizeInChunks() * 32);
		if (coordinate < 0)
			coordinate += world.getSizeInChunks() * 32;
		return coordinate;
	}

	private int sanitizeVerticalCoordinate(int coordinate)
	{
		if (coordinate < 0)
			coordinate = 0;
		if (coordinate >= world.getWorldInfo().getSize().heightInChunks * 32)
			coordinate = world.getWorldInfo().getSize().heightInChunks * 32 - 1;
		return coordinate;
	}

	@Override
	public Iterator<Entity> iterator()
	{
		return backingIterative.iterator();
	}
}
