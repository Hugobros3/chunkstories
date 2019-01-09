//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.world;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import xyz.chunkstories.api.physics.Box;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import xyz.chunkstories.api.Location;
import xyz.chunkstories.api.entity.Entity;
import xyz.chunkstories.api.util.CompoundIterator;
import xyz.chunkstories.api.world.World;
import xyz.chunkstories.api.world.World.NearEntitiesIterator;
import xyz.chunkstories.api.world.chunk.Chunk;
import xyz.chunkstories.api.world.region.Region;

public class WorldEntitiesHolder implements Iterable<Entity> {
	Map<Long, Entity> backing = new ConcurrentHashMap<Long, Entity>();
	ConcurrentLinkedQueue<Entity> backingIterative = new ConcurrentLinkedQueue<Entity>();

	final World world;

	public WorldEntitiesHolder(World world) {
		this.world = world;
	}

	// Elements are sorted in increasing hashcode order
	public void insertEntity(Entity entity) {
		if (backing.put(entity.getUUID(), entity) == null)
			backingIterative.add(entity);
	}

	public boolean removeEntity(Entity entity) {
		if (backing.remove(entity.getUUID()) != null)
			backingIterative.remove(entity);
		else
			System.out.println(
					"Warning, EntitiesHolders was asked to remove entity " + entity + " not found in entities list.");

		return false;
	}

	public Entity getEntityByUUID(long uuid) {
		return backing.get(uuid);
	}

	public NearEntitiesIterator getEntitiesInBox(Vector3dc center, Vector3dc boxSize) {
		int centerVoxel_x = (int) (double) center.x();
		int centerVoxel_y = (int) (double) center.y();
		int centerVoxel_z = (int) (double) center.z();

		int box_ceil_x = (int) Math.ceil((double) boxSize.x());
		int box_ceil_y = (int) Math.ceil((double) boxSize.y());
		int box_ceil_z = (int) Math.ceil((double) boxSize.z());

		int box_start_x = sanitizeHorizontalCoordinate(centerVoxel_x - box_ceil_x);
		int box_start_y = sanitizeVerticalCoordinate(centerVoxel_y - box_ceil_y);
		int box_start_z = sanitizeHorizontalCoordinate(centerVoxel_z - box_ceil_z);

		int box_end_x = sanitizeHorizontalCoordinate(centerVoxel_x + box_ceil_x);
		int box_end_y = sanitizeVerticalCoordinate(centerVoxel_y + box_ceil_y);
		int box_end_z = sanitizeHorizontalCoordinate(centerVoxel_z + box_ceil_z);

		// Chunk-relative

		int csx = box_start_x >> 5;
		int csy = box_start_y >> 5;
		int csz = box_start_z >> 5;

		int cex = box_end_x >> 5;
		int cey = box_end_y >> 5;
		int cez = box_end_z >> 5;

		Box box = new Box(box_start_x, box_start_y, box_start_z, box_end_x - box_start_x,
				box_end_y - box_start_y, box_end_z - box_start_z);
		// System.out.println(box.xw+":"+box.h+":"+box.zw);
		// Vector3d boxc = new Vector3d(box.xpos, box.ypos, box.zpos);
		// System.out.println(boxc+":"+center+":"+boxc.sub(center));

		// Fast path #1: it's all in one chunk!
		if (csx == cex && csy == cey && csz == cez) {
			Chunk chunk = world.getChunk(csx, csy, csz);
			if (chunk != null)
				return new DistanceCheckedIterator(chunk.getEntitiesWithinChunk(), box);
			else
				return new NearEntitiesIterator() {

					@Override
					public boolean hasNext() {
						return false;
					}

					@Override
					public Entity next() {
						throw new UnsupportedOperationException();
					}

					@Override
					public double distance() {
						return -1;
					}

				};
		}

		int rsx = csx >> 3;
		int rsy = csy >> 3;
		int rsz = csz >> 3;

		int rex = cex >> 3;
		int rey = cey >> 3;
		int rez = cez >> 3;

		// Fast path #2: all chunks in the same region
		if (rsx == rex && rsy == rey && rsz == rez) {
			ArrayList<Iterator<Entity>> iterators = new ArrayList<>();
			for (int cx = csx; cx <= cex; cx++)
				for (int cy = csy; cy <= cey; cy++)
					for (int cz = csz; cz <= cez; cz++) {
						// System.out.println(center.x() / 32+":"+center.y() / 32+":"+center.z() / 32);
						// System.out.println(cx+":"+cy+":"+cz);
						Chunk chunk = world.getChunk(cx, cy, cz);
						if (chunk != null)
							iterators.add(chunk.getEntitiesWithinChunk());
					}

			return new DistanceCheckedIterator(new CompoundIterator<>(iterators), box);
		}

		// Slow (and old) path
		return getEntitiesInBoxSlow(center, boxSize);
	}

	class DistanceCheckedIterator implements NearEntitiesIterator {

		public DistanceCheckedIterator(Iterator<Entity> i, Box box) {
			this.i = i;
			this.box = box;

			produce();
		}

		final Iterator<Entity> i;
		final Box box;
		Entity next = null;
		double distance;

		@Override
		public boolean hasNext() {
			produce();
			return next != null;
		}

		private void produce() {
			while (next == null && i.hasNext()) {
				Entity candidate = i.next();
				if (box.isPointInside(candidate.getLocation())) {
					next = candidate;
					distance = candidate.getLocation().distance(new Vector3d(box.xPosition, box.yPosition, box.zPosition));
				}
			}
		}

		@Override
		public Entity next() {
			Entity oldnext = next;
			next = null;
			produce();
			return oldnext;
		}

		@Override
		public double distance() {
			return distance;
		}

	}

	private NearEntitiesIterator getEntitiesInBoxSlow(Vector3dc center, Vector3dc boxSize) {

		return new NearEntitiesIterator() {

			int centerVoxel_x = (int) (double) center.x();
			int centerVoxel_y = (int) (double) center.y();
			int centerVoxel_z = (int) (double) center.z();

			int box_ceil_x = (int) Math.ceil((double) boxSize.x());
			int box_ceil_y = (int) Math.ceil((double) boxSize.y());
			int box_ceil_z = (int) Math.ceil((double) boxSize.z());

			int box_start_x = sanitizeHorizontalCoordinate(centerVoxel_x - box_ceil_x);
			int box_start_y = sanitizeVerticalCoordinate(centerVoxel_y - box_ceil_y);
			int box_start_z = sanitizeHorizontalCoordinate(centerVoxel_z - box_ceil_z);

			int box_end_x = sanitizeHorizontalCoordinate(centerVoxel_x + box_ceil_x);
			int box_end_y = sanitizeVerticalCoordinate(centerVoxel_y + box_ceil_y);
			int box_end_z = sanitizeHorizontalCoordinate(centerVoxel_z + box_ceil_z);

			// We currently sort this out by regions, chunks would be more appropriate ?
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
			Iterator<Entity> currentRegionIterator = currentRegion == null ? null
					: currentRegion.getEntitiesWithinRegion().iterator();
			Entity next = null;
			double distance = 0D;

			private void seekNextEntity() {
				next = null;
				while (true) {
					// Break the loop if we find an entity in the region
					if (seekNextEntityWithinRegion())
						break;
					else {
						// Seek a suitable region if we failed to find anything above
						if (seekNextRegion())
							continue;
						// Break the loop if we are out of regions to check
						else
							break;
					}
				}
			}

			private boolean seekNextEntityWithinRegion() {

				if (currentRegionIterator == null)
					return false;
				while (currentRegionIterator.hasNext()) {
					Entity entity = currentRegionIterator.next();
					// Check if it's inside the box for realz

					Location loc = entity.getLocation();

					int locx = (int) (double) loc.x();
					// Normal case, check if it's in the bounds, wrap-arround case, check if it's
					// outside
					if ((box_start_x > box_end_x) == (locx >= box_start_x && locx <= box_end_x))
						continue;

					int locy = (int) (double) loc.y();
					// Normal case, check if it's in the bounds, wrap-arround case, check if it's
					// outside
					if ((box_start_y > box_end_y) == (locy >= box_start_y && locy <= box_end_y))
						continue;

					int locz = (int) (double) loc.z();
					// Normal case, check if it's in the bounds, wrap-arround case, check if it's
					// outside
					if ((box_start_z > box_end_z) == (locz >= box_start_z && locz <= box_end_z))
						continue;

					// if(Math.abs(check.getX()) <= boxSize.getX() && Math.abs(check.getY()) <=
					// boxSize.getY() && Math.abs(check.getZ()) <= boxSize.getZ())
					{
						// Found a good one
						this.next = entity;

						Vector3d check = new Vector3d(loc);
						check.sub(center);
						this.distance = check.length();
						return true;
					}
				}

				// We found nothing :(
				currentRegionIterator = null;
				return false;
			}

			private boolean seekNextRegion() {
				currentRegion = null;
				while (true) {
					// Found one !
					if (currentRegion != null) {
						currentRegionIterator = currentRegion.getEntitiesWithinRegion().iterator();
						return true;
					}

					region_x++;
					// Wrap arround in X dimension to Y
					if (region_x > region_end_x) {
						region_x = 0;
						region_y++;
					}
					// Then Y to Z
					if (region_y > region_end_y) {
						region_y = 0;
						region_z++;
					}
					// We are done here
					if (region_z > region_end_z)
						return false;

					currentRegion = world.getRegion(region_x, region_y, region_z);
				}
			}

			@Override
			public boolean hasNext() {
				if (next == null)
					seekNextEntity();
				return next != null;
			}

			@Override
			public Entity next() {
				Entity entity = next;
				seekNextEntity();
				return entity;
			}

			@Override
			public double distance() {
				return distance;
			}

		};
	}

	private int sanitizeHorizontalCoordinate(int coordinate) {
		coordinate = coordinate % (world.getSizeInChunks() * 32);
		if (coordinate < 0)
			coordinate += world.getSizeInChunks() * 32;
		return coordinate;
	}

	private int sanitizeVerticalCoordinate(int coordinate) {
		if (coordinate < 0)
			coordinate = 0;
		if (coordinate >= world.getWorldInfo().getSize().heightInChunks * 32)
			coordinate = world.getWorldInfo().getSize().heightInChunks * 32 - 1;
		return coordinate;
	}

	@Override
	public Iterator<Entity> iterator() {
		return backingIterative.iterator();
	}
}
