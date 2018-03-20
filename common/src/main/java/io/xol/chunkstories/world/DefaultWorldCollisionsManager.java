//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world;

import java.util.Iterator;

import org.joml.Vector3d;
import org.joml.Vector3dc;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.physics.CollisionBox;
import io.xol.chunkstories.api.world.WorldCollisionsManager;
import io.xol.chunkstories.api.world.cell.CellData;
import io.xol.chunkstories.world.iterators.EntityRayIterator;

/** Responsible for handling the 'pixel-perfect' AABB collisions */
public class DefaultWorldCollisionsManager implements WorldCollisionsManager
{
	private final WorldImplementation world;
	
	public DefaultWorldCollisionsManager(WorldImplementation world) {
		this.world = world;
	}
	
	@Override
	public Location raytraceSolid(Vector3dc initialPosition, Vector3dc direction, double limit)
	{
		return raytraceSolid(initialPosition, direction, limit, false, false);
	}

	@Override
	public Location raytraceSolidOuter(Vector3dc initialPosition, Vector3dc direction, double limit)
	{
		return raytraceSolid(initialPosition, direction, limit, true, false);
	}

	@Override
	public Location raytraceSelectable(Location initialPosition, Vector3dc direction, double limit)
	{
		return raytraceSolid(initialPosition, direction, limit, false, true);
	}

	private Location raytraceSolid(Vector3dc initialPosition, Vector3dc directionIn, double limit, boolean outer, boolean selectable)
	{
		Vector3d direction = new Vector3d();
		directionIn.normalize(direction);
		
		//direction.scale(0.02);

		//float distance = 0f;
		CellData cell;
		//Voxel vox;
		int x, y, z;
		x = (int) Math.floor(initialPosition.x());
		y = (int) Math.floor(initialPosition.y());
		z = (int) Math.floor(initialPosition.z());

		//DDA algorithm

		//It requires double arrays because it works using loops over each dimension
		double[] rayOrigin = new double[3];
		double[] rayDirection = new double[3];
		rayOrigin[0] = initialPosition.x();
		rayOrigin[1] = initialPosition.y();
		rayOrigin[2] = initialPosition.z();
		rayDirection[0] = direction.x();
		rayDirection[1] = direction.y();
		rayDirection[2] = direction.z();
		int voxelCoords[] = new int[] { x, y, z };
		int voxelDelta[] = new int[] { 0, 0, 0 };
		double[] deltaDist = new double[3];
		double[] next = new double[3];
		int step[] = new int[3];

		int side = 0;
		//Prepare distances
		for (int i = 0; i < 3; ++i)
		{
			double deltaX = rayDirection[0] / rayDirection[i];
			double deltaY = rayDirection[1] / rayDirection[i];
			double deltaZ = rayDirection[2] / rayDirection[i];
			deltaDist[i] = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
			if (rayDirection[i] < 0.f)
			{
				step[i] = -1;
				next[i] = (rayOrigin[i] - voxelCoords[i]) * deltaDist[i];
			}
			else
			{
				step[i] = 1;
				next[i] = (voxelCoords[i] + 1.f - rayOrigin[i]) * deltaDist[i];
			}
		}

		do
		{
			
			//DDA steps
			side = 0;
			for (int i = 1; i < 3; ++i)
			{
				if (next[side] > next[i])
				{
					side = i;
				}
			}
			next[side] += deltaDist[side];
			voxelCoords[side] += step[side];
			voxelDelta[side] += step[side];

			x = voxelCoords[0];
			y = voxelCoords[1];
			z = voxelCoords[2];
			cell = world.peekSafely(x, y, z);
			if (cell.getVoxel().getDefinition().isSolid() || (selectable && cell.getVoxel().getDefinition().isSelectable()))
			{
				boolean collides = false;
				for (CollisionBox box : cell.getTranslatedCollisionBoxes())
				{
					//System.out.println(box);
					Vector3dc collisionPoint = box.lineIntersection(initialPosition, direction);
					if (collisionPoint != null)
					{
						collides = true;
						//System.out.println("collides @ "+collisionPoint);
					}
				}
				if (collides)
				{
					if (!outer)
						return new Location(world, x, y, z);
					else
					{
						//Back off a bit
						switch (side)
						{
						case 0:
							x -= step[side];
							break;
						case 1:
							y -= step[side];
							break;
						case 2:
							z -= step[side];
							break;
						}
						return new Location(world, x, y, z);
					}
				}
			}
			
			//distance += deltaDist[side];

		}
		while (voxelDelta[0] * voxelDelta[0] + voxelDelta[1] * voxelDelta[1] + voxelDelta[2] * voxelDelta[2] < limit * limit);
		return null;
	}

	@Override
	public Iterator<Entity> rayTraceEntities(Vector3dc initialPosition, Vector3dc direction, double limit)
	{
		double blocksLimit = limit;

		Vector3d blocksCollision = this.raytraceSolid(initialPosition, direction, limit);
		if (blocksCollision != null)
			blocksLimit = blocksCollision.distance(initialPosition);

		return raytraceEntitiesIgnoringVoxels(initialPosition, direction, Math.min(blocksLimit, limit));
	}

	@Override
	public Iterator<Entity> raytraceEntitiesIgnoringVoxels(Vector3dc initialPosition, Vector3dc direction, double limit)
	{
		return new EntityRayIterator(world, initialPosition, direction, limit);
	}
	
	/** 
	 * Does a complicated check to see how far the entity can go using the delta direction, from the 'start' position.
	 * Does not actually move anything
	 * Returns the remaining distance in each dimension if it got stuck ( with vec3(0.0, 0.0, 0.0) meaning it can safely move without colliding with anything )
	 */
	public Vector3d runEntityAgainstWorldVoxels(Entity entity, Vector3dc from, Vector3dc delta)
	{
		CellData cell;
		
		//Extract the current position
		Vector3d pos = new Vector3d(from);

		//Keep biggest distanceToTravel for each dimension collisionBox of our entity
		Vector3d maxDistanceToTravel = new Vector3d(0.0);

		Vector3d direction = new Vector3d(delta);
		direction.normalize();

		//Iterate over every box
		for (int r = 0; r < entity.getCollisionBoxes().length; r++)
		{
			// Make a normalized double vector and keep the original length
			Vector3d vec = new Vector3d(delta);
			Vector3d distanceToTravel = new Vector3d(delta);
			double len = vec.length();
			vec.normalize();
			vec.mul(0.25d);
			
			// Do it block per block, face per face
			double distanceTraveled = 0;
			CollisionBox checkerX = entity.getCollisionBoxes()[r].translate(pos.x(), pos.y(), pos.z());
			CollisionBox checkerY = entity.getCollisionBoxes()[r].translate(pos.x(), pos.y(), pos.z());
			CollisionBox checkerZ = entity.getCollisionBoxes()[r].translate(pos.x(), pos.y(), pos.z());

			double stepDistanceX, stepDistanceY, stepDistanceZ;

			while (distanceTraveled < len)
			{
				if (len - distanceTraveled > 0.25)
				{
					//DistanceTraveled is incremented no matter what, for momentum loss while sliding on walls
					distanceTraveled += 0.25;
				}
				else
				{
					vec = new Vector3d(delta);
					vec.normalize();
					vec.mul(len - distanceTraveled);
					distanceTraveled = len;
				}

				stepDistanceX = vec.x();
				stepDistanceY = vec.y();
				stepDistanceZ = vec.z();

				// Z part
				checkerZ = entity.getCollisionBoxes()[r].translate(pos.x(), pos.y(), pos.z() + stepDistanceZ);
				for(int i = (int)Math.floor(pos.x()) - 1; i < (int)Math.ceil(pos.x() + checkerX.xw); i++)
					for(int j = (int)Math.floor(pos.y()) - 1; j < (int)Math.ceil(pos.y() + checkerX.h); j++)
						for(int k = (int)Math.floor(pos.z()) - 1; k < (int)Math.ceil(pos.z() + checkerX.zw); k++)
						{
							cell = world.peekSafely(i, j, k);
							if (cell.getVoxel().getDefinition().isSolid())
							{
								CollisionBox[] boxes = cell.getTranslatedCollisionBoxes();
								if (boxes != null)
									for (CollisionBox box : boxes)
									{
										if (delta.z() != 0.0)
										{
											if (checkerZ.collidesWith(box))
											{
												stepDistanceZ = 0;
												if (delta.z() < 0)
												{
													double south = Math.min((box.zpos + box.zw + checkerZ.zw) - (pos.z()), 0.0d);
													stepDistanceZ = south;
												}
												else
												{
													double north = Math.max((box.zpos) - (pos.z() + checkerZ.zw), 0.0d);
													stepDistanceZ = north;
												}
												vec.z = (0d);
												checkerZ = entity.getCollisionBoxes()[r].translate(pos.x(), pos.y(), pos.z() + stepDistanceZ);
											}
										}
									}
							}
						}
				distanceToTravel.z = (distanceToTravel.z() - stepDistanceZ);
				pos.z = (pos.z() + stepDistanceZ);

				// X-part
				checkerX = entity.getCollisionBoxes()[r].translate(pos.x() + stepDistanceX, pos.y(), pos.z());
				for(int i = (int)Math.floor(pos.x()) - 1; i < (int)Math.ceil(pos.x() + checkerY.xw); i++)
					for(int j = (int)Math.floor(pos.y()) - 1; j < (int)Math.ceil(pos.y() + checkerY.h); j++)
						for(int k = (int)Math.floor(pos.z()) - 1; k < (int)Math.ceil(pos.z() + checkerY.zw); k++)
						{
							cell = world.peekSafely(i, j, k);
							if (cell.getVoxel().getDefinition().isSolid())
							{
								CollisionBox[] boxes = cell.getTranslatedCollisionBoxes();
								if (boxes != null)
									for (CollisionBox box : boxes)
									{
										if (delta.x() != 0.0)
										{
											if (checkerX.collidesWith(box))
											{
												stepDistanceX = 0;
												if (delta.x() < 0)
												{
													double left = Math.min((box.xpos + box.xw + checkerX.xw) - (pos.x()), 0.0d);
													//System.out.println("left:"+left);
													stepDistanceX = left;
												}
												else
												{
													double right = Math.max((box.xpos) - (pos.x() + checkerX.xw), 0.0d);
													//System.out.println("right"+right);
													stepDistanceX = right;
												}
												vec.x = (0d);
												checkerX = entity.getCollisionBoxes()[r].translate(pos.x() + stepDistanceX, pos.y(), pos.z());
											}
										}
									}
							}
						}
				pos.x = (pos.x() + stepDistanceX);
				distanceToTravel.x = (distanceToTravel.x() - stepDistanceX);

				//Y-part
				checkerY = entity.getCollisionBoxes()[r].translate(pos.x(), pos.y() + stepDistanceY, pos.z());
				for(int i = (int)Math.floor(pos.x()) - 1; i < (int)Math.ceil(pos.x() + checkerZ.xw); i++)
					for(int j = (int)Math.floor(pos.y()) - 1; j < (int)Math.ceil(pos.y() + checkerZ.h) + 1; j++)
						for(int k = (int)Math.floor(pos.z()) - 1; k < (int)Math.ceil(pos.z() + checkerZ.zw); k++)
						{
							cell = world.peekSafely(i, j, k);
							if (cell.getVoxel().getDefinition().isSolid())
							{
								CollisionBox[] boxes = cell.getTranslatedCollisionBoxes();
								if (boxes != null)
									for (CollisionBox box : boxes)
									{
										if (delta.y() != 0.0)
										{
											if (checkerY.collidesWith(box))
											{
												stepDistanceY = 0;
												if (delta.y() < 0)
												{
													double top = Math.min((box.ypos + box.h) - pos.y(), 0.0d);
													// System.out.println(top);
													stepDistanceY = top;
												}
												else
												{
													double bot = Math.max((box.ypos) - (pos.y() + checkerY.h), 0.0d);
													// System.out.println(bot);
													stepDistanceY = bot;
												}
												vec.y = (0d);
												checkerY = entity.getCollisionBoxes()[r].translate(pos.x(), pos.y() + stepDistanceY, pos.z());
											}
										}

									}
							}
						}
				pos.y = (pos.y() + stepDistanceY);
				distanceToTravel.y = (distanceToTravel.y() - stepDistanceY);
			}

			if (Math.abs(distanceToTravel.x()) > Math.abs(maxDistanceToTravel.x()))
				maxDistanceToTravel.x = (distanceToTravel.x());

			if (Math.abs(distanceToTravel.y()) > Math.abs(maxDistanceToTravel.y()))
				maxDistanceToTravel.y = (distanceToTravel.y());

			if (Math.abs(distanceToTravel.z()) > Math.abs(maxDistanceToTravel.z()))
				maxDistanceToTravel.z = (distanceToTravel.z());
		}
		return maxDistanceToTravel;
	}
}
